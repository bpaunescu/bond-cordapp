package net.corda.examples.bond.controller

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.bond.model.Bond
import net.corda.examples.bond.model.BondContract
import net.corda.examples.bond.model.BondContract.Companion.BOND_CONTRACT_CLASS_NAME

@InitiatingFlow
@StartableByRPC
class TransferBondFlow(val bondId: UniqueIdentifier,
                       val buyer: Party) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull() ?: throw FlowException("No available notary.")
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(bondId.id))
        val bondTransferInputState = serviceHub.vaultService.queryBy<Bond>(queryCriteria).states.singleOrNull() ?: throw FlowException("Bond with id=$bondId was not found.")
        val bondToBeTransferred = bondTransferInputState.state.data

        check(ourIdentity == bondToBeTransferred.owner) { "Bond transfer can only be initiated by the owner." }

        val transferredBond = bondToBeTransferred.withNewOwner(buyer)
        val signers = setOf(bondToBeTransferred.owner, buyer)
        val signerKeys = signers.map { it.owningKey }
        val transferCommand = Command(BondContract.TransferCommand(), signerKeys)

        val txBuilder = TransactionBuilder(notary).addInputState(bondTransferInputState)
                .addOutputState(transferredBond, BOND_CONTRACT_CLASS_NAME)
                .addCommand(transferCommand)
        txBuilder.verify(serviceHub)

        val signedTx = serviceHub.signInitialTransaction(txBuilder, bondToBeTransferred.owner.owningKey)
        val buyerSession = initiateFlow(buyer)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(buyerSession), CollectSignaturesFlow.tracker()))

        subFlow(FinalityFlow(fullySignedTx))
    }
}
@InitiatedBy(TransferBondFlow::class)
class TransferBondFlowResponder(val buyerSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(buyerSession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "Output must be a bond." using (output is Bond)
                val bond = output as Bond
                "I am the new owner." using (bond.owner == ourIdentity)
            }
        }
        subFlow(signTransactionFlow)
    }
}