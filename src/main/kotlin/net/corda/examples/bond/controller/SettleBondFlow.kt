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
import net.corda.finance.contracts.asset.Cash

@InitiatingFlow
@StartableByRPC
class SettleBondFlow(val bondId: UniqueIdentifier) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull() ?: throw FlowException("No available notary.")
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(bondId.id))
        val bondSettlementInputState = serviceHub.vaultService.queryBy<Bond>(queryCriteria).states.singleOrNull() ?: throw FlowException("Bond with id=$bondId was not found.")
        val bondToBeSettled = bondSettlementInputState.state.data

        check(ourIdentity == bondToBeSettled.issuer) { "Bond must be settled by the issuer." }

        val settleCommand = Command(BondContract.SettleCommand(), bondToBeSettled.participants.map {it.owningKey})

        val txBuilder = TransactionBuilder(notary).addInputState(bondSettlementInputState)
                .addCommand(settleCommand)
        val (_, cashSigningKeys) = Cash.generateSpend(serviceHub, txBuilder, bondToBeSettled.principal, bondToBeSettled.owner)

        txBuilder.verify(serviceHub)
        val signedTx = serviceHub.signInitialTransaction(txBuilder, cashSigningKeys + bondToBeSettled.issuer.owningKey)

        val ownerSession = initiateFlow(bondToBeSettled.owner as Party)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(ownerSession), CollectSignaturesFlow.tracker()))

        subFlow(FinalityFlow(fullySignedTx))
    }
}

@InitiatedBy(SettleBondFlow::class)
class SettleBondFlowResponder(val issuerSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(issuerSession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
               "No outputs should be generated." using (stx.tx.outputs.isEmpty())
            }
        }
        subFlow(signTransactionFlow)
    }
}