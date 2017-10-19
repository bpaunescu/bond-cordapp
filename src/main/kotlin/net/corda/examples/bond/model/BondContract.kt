package net.corda.examples.bond.model

import net.corda.core.contracts.Contract
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCash
import java.math.BigDecimal
import java.security.PublicKey
import java.time.Instant

class BondContract : Contract {

    companion object {
        val BOND_CONTRACT_CLASS_NAME = "net.corda.examples.bond.model.BondContract"
    }

    open class BondCommand : TypeOnlyCommandData()
    class IssueCommand : BondCommand()
    class TransferCommand : BondCommand()
    class PayCouponCommand : BondCommand()
    class SettleCommand : BondCommand()

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<BondCommand>()
        val signers = command.signers
        when (command.value) {
            is IssueCommand -> verifyIssue(tx, signers)
            is TransferCommand -> verifyTransfer(tx, signers)
            is PayCouponCommand -> verifyPayCoupon(tx, signers)
            is SettleCommand -> verifySettle(tx, signers)
        }
    }

    private fun verifyIssue(tx: LedgerTransaction, signers: List<PublicKey>) {
        requireThat {
            "No inputs should be consumed when issuing a Bond." using (tx.inputs.isEmpty())
            "There should be one output state of type Bond." using (tx.outputs.size == 1)

            val out = tx.outputs.single().data as Bond
            "The issuer and owner must be different entities." using (out.issuer != out.owner)
            "The issue date must not be before today." using (out.issueDate.isAfter(Instant.now()))
            "The maturity date must be after the issue date." using (out.maturityDate.isAfter(out.issueDate))
            "The principal value ${out.principal} must be greater than 0." using (out.principal.toDecimal().compareTo(BigDecimal.ZERO) == 1)
            "Coupon rate must be a number between 0 and 100." using (out.couponRate in 0..1)
            "Two parties must sign." using (signers.toSet().size == 2)
            "The issuer must be one of the signers." using (signers.contains(out.issuer.owningKey))
            "The buyer must be one of the signers." using (signers.contains(out.owner.owningKey))
        }


    }

    private fun verifyTransfer(tx: LedgerTransaction, signers: List<PublicKey>) {
        requireThat {
            "There should be one input state of type Bond." using (tx.inputs.size == 1)
            "There should be one output state of type Bond." using (tx.outputs.size == 1)

            val input = tx.inputStates.single() as Bond
            val output = tx.outputStates.single() as Bond
            "The owner of the bond must change after the transfer." using (input.owner != output.owner)
            "Two parties must sign." using (signers.toSet().size == 2)
            "Previous owner must be one of the signers." using (signers.toSet().contains(input.owner.owningKey))
            "New owner must be one of the signers." using (signers.toSet().contains(output.owner.owningKey))
            //TODO: add a check for cash exchange, maybe?
        }
    }

    private fun verifyPayCoupon(tx: LedgerTransaction, signers: List<PublicKey>) {

    }

    private fun verifySettle(tx: LedgerTransaction, signers: List<PublicKey>) {
        requireThat {
            "There should be one input state of type Bond." using (tx.inputs.size == 1)
            "There should be one output state." using (tx.outputs.size == 1)
            val inputBond = tx.inputs.single() as Bond
            val cash = tx.outputsOfType<Cash.State>()
            val acceptableCash = cash.filter { it.owner == inputBond.owner}
            "The output state must be of type Cash." using (cash.isNotEmpty())
            "The cash must be paid to the owner of the bond." using (acceptableCash.isNotEmpty())
            "The amount settled must be equal to the value of the bond principal" using (inputBond.principal == acceptableCash.sumCash())
            "Two parties must sign." using (signers.toSet().size == 2)
            "One of the signers must be the issuer of the bond." using (signers.toSet().contains(inputBond.issuer.owningKey))
            "One of the signers must be the owner of the bond." using (signers.toSet().contains(inputBond.owner.owningKey))
        }
    }
}