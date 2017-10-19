package net.corda.examples.bond.controller

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.examples.bond.model.Bond
import net.corda.examples.bond.model.BondContract
import net.corda.examples.bond.model.CouponType
import java.time.Instant
import java.util.*
import kotlin.reflect.jvm.jvmName

@InitiatingFlow
@StartableByRPC
class IssueBondFlow(val buyer: Party,
                    val principal: Amount<Currency>,
                    val issueDate: Instant,
                    val maturityDate: Instant,
                    val couponType: CouponType,
                    val couponRate: Double) : FlowLogic<Unit>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call() {
        // Initialize transaction components.
        val bond = Bond(ourIdentity, buyer, issueDate, maturityDate, principal, couponRate, couponType)
        val notary = serviceHub.networkMapCache.notaryIdentities.firstOrNull() ?: throw FlowException("No available notary.")
        val bondContract = BondContract::class.jvmName
        val bondContractAndState = StateAndContract(bond, bondContract)
        val command = Command(BondContract.IssueCommand(), listOf(ourIdentity.owningKey, buyer.owningKey))

        // Build and verify transaction.
        val txBuilder = TransactionBuilder(notary).withItems(bondContractAndState, command)
        txBuilder.verify(serviceHub)

        // Sign the transaction.
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        val buyerSession = initiateFlow(buyer)
        val fullySignedTx = subFlow(CollectSignaturesFlow(signedTx, listOf(buyerSession), CollectSignaturesFlow.tracker()))

        subFlow(FinalityFlow(fullySignedTx))
    }
}

@InitiatedBy(IssueBondFlow::class)
class IssueBondFlowResponder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "Output must be a Bond" using (output is Bond)
            }
        }
        subFlow(signTransactionFlow)
    }
}