![Corda](https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png)

# The Bond CorDapp

This is an example of how to develop a CorDapp for bond issuance, transfers and settlements . It implements a simple bond state with coupons that can be redeemed weekly, monthly, quarterly, semi-annually or annually.  

## Build

```
./gradlew build
```

The output will be in `build/libs`.

## Dependencies

The Bond CorDapp depends on the [Finance CorDapp](https://dl.bintray.com/r3/corda/net/corda/corda-finance/1.0.0/corda-finance-1.0.0.jar). Put both in the plugins directory on your Corda node.

