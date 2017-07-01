package com.zuora.api.util;

import com.zuora.api.axis2.UnexpectedErrorFault;
import com.zuora.api.axis2.ZuoraServiceStub;
import com.zuora.api.axis2.ZuoraServiceStub.*;
import com.zuora.api.util.dto.ChargeModelType;
import com.zuora.api.util.dto.PriceAndDiscountRequest;
import com.zuora.api.util.dto.PriceAndDiscountResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

public class ZApiTest {

    final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private static Logger logger = LoggerFactory.getLogger(ZApiTest.class);
    private ZApi zapi;

    @Before
    public void setUp() {
        zapi = new ZApi();
    }

    @Test
    public void testCreatingZApi() {
        // Test the endpoint
        Assert.assertNotEquals(zapi.getEndpoint(), "");
    }

    @Test
    public void testZLogin() {
        LoginResult loginResult = zapi.zLogin();
        Assert.assertTrue(loginResult.getSession() != null);
    }

    @Test
    public void testZQuery() {
        zapi.zLogin();
        QueryResult queryResult = zapi.zQuery("SELECT AccountNumber FROM Account WHERE Id = 'dummyId'");
        Assert.assertEquals(queryResult.getSize(), 0);
    }

    @Test
    public void testZSubscribe() throws UnexpectedErrorFault, RemoteException, ParseException {
        zapi.zLogin();

        final SubscribeResult[] result = createSubscription();

        Assert.assertNotNull(result);
        Assert.assertEquals(result[0].getGatewayResponseCode(), "Approved");
        Assert.assertTrue(result[0].getSuccess());
    }

    private SubscribeResult[] createSubscription() throws ParseException, UnexpectedErrorFault, RemoteException {
        SubscriptionData data = new SubscriptionData();
        data.setSubscription(makeSubscription());
        data.setRatePlanData(makeRatePlanData());

        SubscribeRequest request = new SubscribeRequest();
        request.setAccount(makeAccount());
        request.setBillToContact(makeContact()); //CONDITIONAL
        request.setSoldToContact(makeContact()); //NO
        request.setPaymentMethod(makePaymentMethod()); // NO
        request.setSubscriptionData(data);

        return zapi.zSubscribe(new SubscribeRequest[]{request});
    }

    @Test
    public void testCancelSubscriptionDefaultAmendOptions() throws UnexpectedErrorFault, RemoteException, ParseException {
        zapi.zLogin();

        // create a subscription first
        final SubscribeResult[] results = createSubscription();
        Assert.assertNotNull(results[0].getSubscriptionId());
        System.out.println("subscription id: " + results[0].getSubscriptionId());
        System.out.println("subscription number: " + results[0].getSubscriptionNumber());
        final ID subscriptionId = results[0].getSubscriptionId();

        if (subscriptionId != null) {
            // Cancel the subscription
            final ZuoraServiceStub.Amendment amendment = new ZuoraServiceStub.Amendment();
            amendment.setType("Cancellation");
            amendment.setName("Test Cancel Amendment");
            amendment.setSubscriptionId(subscriptionId);
            amendment.setContractEffectiveDate(sdf.format(new Date()));
            amendment.setEffectiveDate(makeSubscription().getContractEffectiveDate());

            final ZuoraServiceStub.AmendResult[] amendResults = zapi.zAmend(new ZuoraServiceStub.Amendment[]{amendment});

            Assert.assertNotNull(amendResults);
            Assert.assertNotNull(amendResults[0].getAmendmentIds());
            Assert.assertTrue(amendResults[0].getSuccess());
        }

    }

    @Test
    public void testCancelSubscriptionCustomAmendOptions() throws UnexpectedErrorFault, RemoteException, ParseException {
        zapi.zLogin();

        // create a subscription first
        final SubscribeResult[] results = createSubscription();
        Assert.assertNotNull(results[0].getSubscriptionId());
        System.out.println("subscription id: " + results[0].getSubscriptionId());
        System.out.println("subscription number: " + results[0].getSubscriptionNumber());
        final ID subscriptionId = results[0].getSubscriptionId();

        if (subscriptionId != null) {
            // Cancel the subscription
            final ZuoraServiceStub.Amendment amendment = new ZuoraServiceStub.Amendment();
            amendment.setType("Cancellation");
            amendment.setName("Test Cancel Amendment");
            amendment.setSubscriptionId(subscriptionId);
            amendment.setContractEffectiveDate(sdf.format(new Date()));
            amendment.setEffectiveDate(makeSubscription().getContractEffectiveDate());

            AmendOptions amendOptions = new AmendOptions();
            amendOptions.setGenerateInvoice(false);
            amendOptions.setProcessPayments(false);

            final ZuoraServiceStub.AmendResult[] amendResults = zapi.zAmend(new ZuoraServiceStub.Amendment[]{amendment}, amendOptions);

            Assert.assertNotNull(amendResults);
            Assert.assertNotNull(amendResults[0].getAmendmentIds());
            Assert.assertTrue(amendResults[0].getSuccess());
        }

    }


    @Test
    public void createWithError() {

        zapi.zLogin();

        Account account = new Account();
        account.setName("Dummy account");

        SaveResult[] result = zapi.zCreate(new ZObject[]{(ZObject) account});

        // Make sure the result throw an error
        Assert.assertFalse(result[0].getSuccess());

        logger.info("Save Result error message (expected) = " + result[0].getErrors()[0].getMessage());
    }

    @Test
    public void createAndDeleteAccount() {

        zapi.zLogin();

        logger.info("Starting account creation");

        Account account = new Account();

        account.setName("Test Account with API");
        account.setCurrency("USD");
        account.setBillCycleDay(1);
        account.setStatus("Draft");

        SaveResult[] result = zapi.zCreate(new ZObject[]{(ZObject) account});

        // Make sure we created the account
        Assert.assertTrue(result[0].getSuccess());

        String accountId = result[0].getId().getID();
        logger.info("Successfully account in draft status with ID = " + accountId);

        // Delete this account
        DeleteResult[] deleteResult = zapi.zDelete(new String[]{accountId}, "Account");

        // Make sure we deleted this test account
        Assert.assertTrue(deleteResult[0].getSuccess());

        logger.info("Successfully deleted the test account");
    }

    @Test
    public void createThenUpdateAndDeleteAccount() {

        zapi.zLogin();

        logger.info("Starting account creation");

        Account account = new Account();

        account.setName("Test Account with API");
        account.setCurrency("USD");
        account.setBillCycleDay(1);
        account.setStatus("Draft");

        SaveResult[] result = zapi.zCreate(new ZObject[]{(ZObject) account});

        // Make sure we created the account
        Assert.assertTrue(result[0].getSuccess());

        String accountId = result[0].getId().getID();
        logger.info("Successfully account in draft status with ID = " + accountId);

        // Update this account
        Account accountToUpdate = new Account();

        ID toUpdateID = new ID();
        toUpdateID.setID(accountId);

        String newName = "Updated Account with API";
        accountToUpdate.setName(newName);
        accountToUpdate.setId(toUpdateID);

        // Update in Zuora
        SaveResult[] updateResults = zapi.zUpdate(new ZObject[]{(ZObject) accountToUpdate});

        for (SaveResult updated : updateResults) {
            Assert.assertTrue(updated.getSuccess());
        }

        logger.info("Successfully updated the test account");

        // Now query Zuora and make sure the name matches
        QueryResult queryResult = zapi.zQuery("SELECT Name FROM Account WHERE Id = '" + accountId + "'");

        for (ZObject obj : queryResult.getRecords()) {
            Account acc = (Account) obj;
            Assert.assertEquals(newName, acc.getName());
        }

        // Delete this account
        DeleteResult[] deleteResult = zapi.zDelete(new String[]{accountId}, "Account");

        // Make sure we deleted this test account
        Assert.assertTrue(deleteResult[0].getSuccess());

        logger.info("Successfully deleted the test account");

    }

    @Test
    public void zAdvancedQuery() {
        ZApi zapi = new ZApi();

        // This should be called before any other call
        zapi.zLogin();

        QueryResult resultado = zapi.zQuery("select id from invoiceitem");

        // Example on how to do a query
        List result = zapi.zAdvancedQuery("select id from invoiceitem");

        Assert.assertEquals(result.size(), resultado.getSize());

    }


    @Test
    public void filterProduct() {

        ZApi zapi = new ZApi();

        // This should be called before any other call
        zapi.zLogin();

        // Example on how to do a query
        QueryResult result = zapi.zQuery("Select id, titlekey__c from ProductRatePlan where id= '2c92c0f9555351c1015558ac83b36b31'");
    }

    /**
     * Make account.
     *
     * @return the account
     */
    private Account makeAccount() {
        long time = System.currentTimeMillis();
        Account acc = new Account();
        acc.setAccountNumber("T-" + time); // string
        acc.setBatch("Batch1"); // enum
        acc.setBillCycleDay(0); // int
        acc.setBcdSettingOption("AutoSet");
        acc.setAllowInvoiceEdit(true); // boolean
        acc.setAutoPay(false);
        acc.setCrmId("SFDC-" + time);
        acc.setCurrency("USD"); // standard DB enum
        acc.setCustomerServiceRepName("CSR Dude");
        acc.setName("ACC-" + time);
        acc.setPurchaseOrderNumber("PO-" + time);
        acc.setSalesRepName("Sales Dude");
        acc.setPaymentTerm("Due Upon Receipt");
        acc.setStatus("Draft");
        return acc;
    }

    /**
     * Make contact.
     *
     * @return the contact
     */
    private Contact makeContact() {
        long time = System.currentTimeMillis();
        Contact con = new Contact();
        con.setFirstName("Firstly" + time);
        con.setLastName("Secondly" + time);
        con.setAddress1("52 Vexford Lane");
        con.setCity("Anaheim");
        con.setState("California");
        con.setCountry("United States");
        con.setPostalCode("92808");
        con.setWorkEmail("contact@test.com");
        con.setWorkPhone("4152225151");
        return con;
    }

    /**
     * Make payment method.
     *
     * @return the payment method
     */
    private PaymentMethod makePaymentMethod() {
        PaymentMethod pm = new PaymentMethod();
        pm.setType("CreditCard");
        pm.setCreditCardType("Visa");
        pm.setCreditCardAddress1("52 Vexford Lane");
        pm.setCreditCardCity("Anaheim");
        pm.setCreditCardState("California");
        pm.setCreditCardPostalCode("92808");
        pm.setCreditCardCountry("United States");
        pm.setCreditCardHolderName("Firstly Lastly");
        pm.setCreditCardExpirationYear(2017);
        pm.setCreditCardExpirationMonth(12);
        pm.setCreditCardNumber("4111111111111111");
        return pm;
    }

    /**
     * Make payment method ach.
     *
     * @return the payment method
     */
    private PaymentMethod makePaymentMethodACH() {
        PaymentMethod pm = new PaymentMethod();
        pm.setType("ACH");
        pm.setAchAbaCode("123123123");
        pm.setAchAccountName("testAccountName");
        pm.setAchAccountNumber("23232323232323");
        pm.setAchAccountType("Saving");
        pm.setAchBankName("Test Bank");
        pm.setCreatedDate(Calendar.getInstance());
        return pm;
    }

    /**
     * Creates a Subscription object reading the values from the property.
     *
     * @return Subscription
     */
    private Subscription makeSubscription() throws ParseException {

        Date date = new Date();

        String dateInDatabaseFormat = sdf.format(date);

        System.out.println("dateInDatabaseFormat: " + dateInDatabaseFormat);


        Subscription sub = new Subscription();
        sub.setName("SUB-" + System.currentTimeMillis());
        sub.setTermStartDate(dateInDatabaseFormat);
        // set ContractEffectiveDate = current date to generate invoice
        // Generates invoice at the time of subscription creation. uncomment for
        // invoice generation
        sub.setContractEffectiveDate(dateInDatabaseFormat);
        sub.setServiceActivationDate(dateInDatabaseFormat);
        // set IsInvoiceSeparate=true //To generate invoice separate for every
        // subscription
        sub.setIsInvoiceSeparate(true);
        sub.setAutoRenew(true);
        sub.setInitialTerm(12);// sets value for next renewal date
        sub.setRenewalTerm(12);
        sub.setNotes("This is a test subscription");
        return sub;
    }

    /**
     * Make rate plan data.
     *
     * @return the rate plan data[]
     */
    public RatePlanData[] makeRatePlanData() {
        RatePlanData ratePlanData = new RatePlanData();
        RatePlan ratePlan = new RatePlan();

        ID id = new ID();
        id.setID("2c92c0f9552e602201552f62f5145984");
        ratePlan.setProductRatePlanId(id);
        ratePlanData.setRatePlan(ratePlan);

        return new RatePlanData[]{ratePlanData};
    }

    @Test
    public void testCreatePaymentMethod() throws ParseException, UnexpectedErrorFault, RemoteException {
        ID id = new ID();
        id.setID("2c92c0f85bae511c015bd2efd9245d78");
        zapi.zLogin();

        Account account = new Account();
        account.setId(id);
        account.setPaymentGateway("Stripe Gateway");
        account.setAutoPay(false);
        account.addFieldsToNull("DefaultPaymentMethodId");

        SaveResult[] updateResults = zapi.zUpdate(new ZObject[]{(ZObject) account});
        Assert.assertTrue(updateResults[0].getSuccess());


        PaymentMethod paymentMethodRequest = makePaymentMethod();
        paymentMethodRequest.setAccountId(id);

        SaveResult[] result = zapi.zCreate(new ZObject[]{(ZObject) paymentMethodRequest});
        Assert.assertTrue(result[0].getSuccess());
        ID paymentMethodId = result[0].getId();


        Account newAccount = new Account();
        newAccount.setId(id);
        newAccount.setDefaultPaymentMethodId(paymentMethodId);
        newAccount.setAutoPay(true);
        account.setBcdSettingOption("ManualSet");
        account.setBillCycleDay(LocalDate.now().getDayOfMonth());
//		account.addFieldsToNull("");

        SaveResult[] updateResultsFinal = zapi.zUpdate(new ZObject[]{(ZObject) newAccount});
        Assert.assertTrue(updateResultsFinal[0].getSuccess());

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(paymentMethodId);

        SubscriptionData data = new SubscriptionData();
        data.setSubscription(makeSubscription());
        data.setRatePlanData(makeRatePlanData());

        SubscribeRequest request = new SubscribeRequest();
        request.setAccount(newAccount);
        request.setPaymentMethod(paymentMethod); // NO
        request.setSubscriptionData(data);

        zapi.zSubscribe(new SubscribeRequest[]{request});
    }

    /**
     * Make rate plan data.
     *
     * @return the rate plan data[]
     */
    public RatePlanData makeRatePlanDataDiscount100() {

        RatePlanData ratePlanData = new RatePlanData();
        RatePlan ratePlan = new RatePlan();

        ID id = new ID();
        id.setID("2c92c0f95c490174015c539541f26dae");
        ratePlan.setProductRatePlanId(id);

        ratePlanData.setRatePlan(ratePlan);

        return ratePlanData;
    }

    @Test
    public void testDescuento2x1() throws ParseException, UnexpectedErrorFault, RemoteException {
        zapi.zLogin();

        final SubscribeResult[] result = createSubscription();

        Assert.assertNotNull(result);
        Assert.assertEquals(result[0].getGatewayResponseCode(), "Approved");
        Assert.assertTrue(result[0].getSuccess());

        QueryResult queryResult = zapi.zQuery("SELECT SubscriptionEndDate FROM Subscription WHERE Id = '" + result[0].getSubscriptionId().getID() + "'");

        System.out.println(((Subscription) queryResult.getRecords()[0]).getSubscriptionEndDate());
        final ZuoraServiceStub.Amendment amendment = new ZuoraServiceStub.Amendment();
        amendment.setType("NewProduct");
        amendment.setName("Added 2x1 Free first renewal");
        amendment.setSubscriptionId(result[0].getSubscriptionId());
        amendment.setContractEffectiveDate(((Subscription) queryResult.getRecords()[0]).getSubscriptionEndDate());
        amendment.setRatePlanData(makeRatePlanDataDiscount100());

        final ZuoraServiceStub.AmendResult[] amendResults = zapi.zAmend(new ZuoraServiceStub.Amendment[]{amendment});

    }

    @Test
    public void testGetPriceAndDiscount() throws Exception {

        final PriceAndDiscountRequest priceAndDiscountRequest = PriceAndDiscountRequest.Builder.aPriceAndDiscountRequest()
                .currency("EUR")
                .ratePlans(new String[]{"2c92c0f9552e6022015530268f953190", "2c92c0f8550f92e2015526a61cd65ad8"})
                .build();


        Assert.assertNotNull(priceAndDiscountRequest);
        Assert.assertTrue(priceAndDiscountRequest.getRatePlans().length > 1);
        Assert.assertTrue(priceAndDiscountRequest.getRatePlans().length < 3);

        PriceAndDiscountResponse priceAndDiscountResponse = new PriceAndDiscountResponse();

        zapi.zLogin();

        for (String ratePlan : priceAndDiscountRequest.getRatePlans()) {
            logger.info("Looking for in ProductRatePlanCharge for ProductRatePlanId: '{}' ", ratePlan);
            QueryResult productRatePlanChargeQueryResult = zapi.zQuery(String.format("select id,ChargeModel from ProductRatePlanCharge where ProductRatePlanId= '%s'", ratePlan));

            if (productRatePlanChargeQueryResult.getSize() == 0) {
                //There is no ProductRatePlanCharge  with this ProductRatePlanId
                logger.error("There is no ProductRatePlanCharge  with this ProductRatePlanId");
                throw new Exception("There is no ProductRatePlanCharge with this ProductRatePlanId");
            }

            String productRatePlanChargeId = productRatePlanChargeQueryResult.getRecords()[0].getId().toString();

            logger.info("Looking for in ProductRatePlanChargeTier for ProductRatePlanChargeId: '{}' with ChargeModel: '{}", productRatePlanChargeId,((ProductRatePlanCharge) productRatePlanChargeQueryResult.getRecords()[0]).getChargeModel());
            if (((ProductRatePlanCharge) productRatePlanChargeQueryResult.getRecords()[0]).getChargeModel().equals(ChargeModelType.PER_UNIT_PRICING.getChargeModel())) {
                QueryResult ProductRatePlanChargeTierQueryResult = zapi.zQuery(String.format("select id,price,Currency from ProductRatePlanChargeTier where ProductRatePlanChargeId = '%s'", productRatePlanChargeId));

                if (ProductRatePlanChargeTierQueryResult.getSize() == 0) {
                    //There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId
                    logger.error("There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId");
                    throw new Exception("There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId");
                }

                priceAndDiscountResponse.setPrice(Stream.of(ProductRatePlanChargeTierQueryResult.getRecords())
                        .filter(record -> ((ProductRatePlanChargeTier) record).getCurrency().equals(priceAndDiscountRequest.getCurrency()))
                        .map(record -> ((ProductRatePlanChargeTier) record).getPrice())
                        .findFirst()
                        .orElseThrow(() -> {
                            logger.error("There is no price for the currency: '{}'", priceAndDiscountRequest.getCurrency());
                            return new Exception(String.format("There is no price for the currency: '%s'", priceAndDiscountRequest.getCurrency()));
                        }));

                logger.info("Found price '{}' for currency '{}'", priceAndDiscountResponse.getPrice(),priceAndDiscountRequest.getCurrency());

            } else {
                QueryResult ProductRatePlanChargeTierQueryResult = zapi.zQuery(String.format("select id,discountPercentage,Currency from ProductRatePlanChargeTier where ProductRatePlanChargeId = '%s'", productRatePlanChargeId));

                if (ProductRatePlanChargeTierQueryResult.getSize() == 0) {
                    //There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId
                    logger.error("There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId");
                    throw new Exception("There is no ProductRatePlanChargeTier  with this ProductRatePlanChargeId");
                }

                priceAndDiscountResponse.setDiscount(Stream.of(ProductRatePlanChargeTierQueryResult.getRecords())
                        .filter(record -> ((ProductRatePlanChargeTier) record).getCurrency().equals(priceAndDiscountRequest.getCurrency()))
                        .map(record -> ((ProductRatePlanChargeTier) record).getDiscountPercentage())
                        .findFirst()
                        .orElseThrow(() -> {
                            logger.error("There is no discount percentage for the currency: '{}'", priceAndDiscountRequest.getCurrency());
                            return new Exception(String.format("There is no discount percentage for the currency: '%s'", priceAndDiscountRequest.getCurrency()));
                        }));

                logger.info("Found discount '{}' for currency '{}'", priceAndDiscountResponse.getDiscount(),priceAndDiscountRequest.getCurrency());
            }


        }

        Assert.assertTrue(priceAndDiscountResponse.getPrice().toString().equals("74.99"));
        Assert.assertTrue(priceAndDiscountResponse.getDiscount().toString().equals("60"));

        logger.info("Results, Price: '{}' Discount: '{}'", priceAndDiscountResponse.getPrice(),priceAndDiscountResponse.getDiscount());

    }


    @Test
    public void testUpgrade() throws Exception {
        zapi.zLogin();
        final SubscribeResult[] result = createSubscription();

        final LocalDateTime currentTime = LocalDateTime.now();
        final String userId = "10246874";

        // 12 Months, we will need to get from a call to external-service
        final List<ID> productRatePlansToUpgrade = new ArrayList();

        ID ratePlanproductId = new ID();
        ratePlanproductId.setID("2c92c0f9552e6022015530268f953190");

        ID ratePlanDtoId = new ID();
        ratePlanDtoId.setID("2c92c0f8550f92e2015526a61cd65ad8");

        productRatePlansToUpgrade.add(ratePlanproductId);  //Product
        productRatePlansToUpgrade.add(ratePlanDtoId);  //Dto


        ID subscriptionToUpgrade = null;
        String subscriptionEndDate = null;
        Integer currentTerm = 12;
        String currentTermPeriodType = "Month";

//        QueryResult accountQueryResult = zapi.zQuery("SELECT Id FROM Account WHERE aba_userId__c = '" + userId + "'");
//
//        if (accountQueryResult.getSize() != 1) {
//            //More than one Active ?
//            logger.info("More than one user account with this userId");
//            throw new Exception("More than one user account with this userId");
//        }
//
//
//        final ID accountId = accountQueryResult.getRecords()[0].getId();

        //SubscriptionEndDate is equal to "Next Renewal Date"
        QueryResult subscriptionQueryResult = zapi.zQuery("SELECT Id,SubscriptionEndDate,CurrentTerm,CurrentTermPeriodType FROM Subscription WHERE Id = '" + result[0].getSubscriptionId().getID() + "' and Status='Active'");

        if (subscriptionQueryResult.getSize() == 0) {
            //There is no subscription to upgrade
            logger.info("There is no subscription to upgrade");
            throw new Exception("There is no subscription to upgrade");
        }

        if (subscriptionQueryResult.getSize() > 1) {
            //More than one Active ?
            logger.info("More than one subscription active");
            throw new Exception("More than one subscription active");
        }

        if (subscriptionQueryResult.getSize() == 1) {
            //Normal process, Active subscription
            logger.info("Subscription Active");
            subscriptionToUpgrade = subscriptionQueryResult.getRecords()[0].getId();
            subscriptionEndDate = ((Subscription) subscriptionQueryResult.getRecords()[0]).getSubscriptionEndDate();
        }

//        Assert.assertNotNull(subcriptionToUpgrade);

        logger.info("Subscription to update: " + subscriptionToUpgrade.toString());

        //We obtain all ratePlans that the subscription has.
        QueryResult ratePlanQueryResult = zapi.zQuery(String.format("SELECT Id FROM RatePlan WHERE SubscriptionId= '%s'", subscriptionToUpgrade.toString()));

        final List<ID> ratePlansToRemove = new ArrayList();

        for (ZObject ratePlan : ratePlanQueryResult.getRecords()) {
            ratePlansToRemove.add(ratePlan.getId());
        }

        // First amend changing the terms and  onditions

        List<Amendment> amendmentsToApply = new ArrayList<Amendment>();

        ZuoraServiceStub.Amendment amendment;

        amendment = new ZuoraServiceStub.Amendment();
        amendment.setType("TermsAndConditions");
        amendment.setName("Upgrade Terms And Conditions");
        amendment.setSubscriptionId(subscriptionToUpgrade);

        amendment.setContractEffectiveDate(subscriptionEndDate);
        amendment.setServiceActivationDate(subscriptionEndDate);
        amendment.setCustomerAcceptanceDate(subscriptionEndDate);
        amendment.setTermStartDate(subscriptionEndDate);

        amendment.setCurrentTerm(currentTerm);
        amendment.setCurrentTermPeriodType(currentTermPeriodType);
        amendment.setRenewalTerm(currentTerm);
        amendment.setRenewalTermPeriodType(currentTermPeriodType);

        amendment.setAutoRenew(true);

        amendmentsToApply.add(amendment);

        // Remove old ratePlans

        for (ID id : ratePlansToRemove) {
            RatePlanData ratePlanData = new RatePlanData();
            RatePlan ratePlan = new RatePlan();

            amendment = new ZuoraServiceStub.Amendment();
            amendment.setType("RemoveProduct");
            amendment.setName("Upgrade Remove product");
            amendment.setSubscriptionId(subscriptionToUpgrade);
            amendment.setContractEffectiveDate(subscriptionEndDate);

            ratePlan.setAmendmentSubscriptionRatePlanId(id);
            ratePlanData.setRatePlan(ratePlan);

            amendment.setRatePlanData(ratePlanData);

            amendmentsToApply.add(amendment);
        }

        // Add new ratePlans

        for (ID id : productRatePlansToUpgrade) {
            RatePlanData ratePlanData = new RatePlanData();
            RatePlan ratePlan = new RatePlan();

            amendment = new ZuoraServiceStub.Amendment();
            amendment.setType("NewProduct");
            amendment.setName("Upgrade New product");
            amendment.setSubscriptionId(subscriptionToUpgrade);

            amendment.setContractEffectiveDate(subscriptionEndDate);
            amendment.setCustomerAcceptanceDate(subscriptionEndDate);
            amendment.setServiceActivationDate(subscriptionEndDate);

            ratePlan.setProductRatePlanId(id);
            ratePlanData.setRatePlan(ratePlan);

            amendment.setRatePlanData(ratePlanData);

            amendmentsToApply.add(amendment);
        }


        Amendment[] amendments = new ZuoraServiceStub.Amendment[amendmentsToApply.size()];
        amendmentsToApply.toArray(amendments);

        final ZuoraServiceStub.AmendResult[] amendResults = zapi.zAmend(amendments);


        for (AmendResult amendResult : amendResults) {
            if (amendResult.getSuccess()) {
                logger.info("Todo OK");
            } else {
                logger.info("Todo KO");
            }
        }


    }

    @Test
    public void test() {
        ID id = new ID();
        id.setID("2c92c0f95bae6219015bc8c416915831");
        zapi.zLogin();

        Account account = new Account();
        account.setId(id);
        account.setBcdSettingOption("ManualSet");
        account.setBillCycleDay(LocalDate.now().getDayOfMonth());

        SaveResult[] updateResults = zapi.zUpdate(new ZObject[]{(ZObject) account});
        Assert.assertTrue(updateResults[0].getSuccess());
    }

    @Test
    public void updateAccountStatus() {

        zapi.zLogin();

        logger.info("Starting account creation");

        Account account = makeAccount();

        account.setName("JMJ-" + account.getName());

        SaveResult[] result = zapi.zCreate(new ZObject[]{(ZObject) account});
        // Make sure we created the account
        Assert.assertTrue(result[0].getSuccess());
        String accountId = result[0].getId().getID();

        //We create a needed soldTo to can active the account and reuse with billTo needed too.

        ID newID = new ID();
        newID.setID(accountId);

        Contact soldTo = makeContact();
        soldTo.setAccountId(newID);

        SaveResult[] soldToResult = zapi.zCreate(new ZObject[]{(ZObject) soldTo});
        Assert.assertTrue(soldToResult[0].getSuccess());
        String soldToId = soldToResult[0].getId().getID();

        ID newSoldToId = new ID();
        newSoldToId.setID(soldToId);

        account.setSoldToId(newSoldToId);
        account.setBillToId(newSoldToId);

        logger.info("Successfully account in draft status with ID = " + accountId);

        logger.info("Check if he's in draft status");

        QueryResult queryResult = zapi.zQuery("SELECT status FROM Account WHERE Id = '" + accountId + "'");

        for (ZObject obj : queryResult.getRecords()) {
            Account acc = (Account) obj;
            Assert.assertEquals("Draft", acc.getStatus());
        }

        logger.info("Set active");

        account.setId(newID);
        account.setStatus("Active");

        // Update in Zuora
        SaveResult[] updateResults = zapi.zUpdate(new ZObject[]{(ZObject) account});

        for (SaveResult updated : updateResults) {
            Assert.assertTrue(updated.getSuccess());
        }

        logger.info("Check if he's in active status");

        queryResult = zapi.zQuery("SELECT status FROM Account WHERE Id = '" + accountId + "'");

        for (ZObject obj : queryResult.getRecords()) {
            Account acc = (Account) obj;
            Assert.assertEquals("Active", acc.getStatus());
        }

        logger.info("Set cancel");

        account.setStatus("Canceled");

        // Update in Zuora
        updateResults = zapi.zUpdate(new ZObject[]{(ZObject) account});

        for (SaveResult updated : updateResults) {
            Assert.assertTrue(updated.getSuccess());
        }

        logger.info("Check if he's in canceled status");

        queryResult = zapi.zQuery("SELECT status FROM Account WHERE Id = '" + accountId + "'");

        for (ZObject obj : queryResult.getRecords()) {
            Account acc = (Account) obj;
            Assert.assertEquals("Canceled", acc.getStatus());
        }


        // Delete this account
        DeleteResult[] deleteResult = zapi.zDelete(new String[]{accountId}, "Account");

        // Make sure we deleted this test account
        Assert.assertTrue(deleteResult[0].getSuccess());

        logger.info("Successfully deleted the test account");


    }
}
