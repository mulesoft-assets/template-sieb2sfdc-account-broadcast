/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.context.notification.NotificationException;
import org.mule.modules.siebel.api.model.response.CreateResult;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Prober;
import org.mule.templates.test.utils.ListenerProbe;
import org.mule.templates.test.utils.PipelineSynchronizeListener;
import org.mule.transport.NullPayload;
import org.mule.util.UUID;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the Mule Template that make calls to external systems.
 * 
 * The test will invoke the batch process and afterwards check that the accounts had been correctly created and that the ones that should be filtered are not in
 * the destination sand box.
 * 
 * The test validates that no account will get sync as result of the integration.
 * 
 * @author damiansima
 * @author MartinZdila
 */
public class BusinessLogicIT extends AbstractTemplatesTestCase {

	private static final Logger log = LogManager.getLogger(BusinessLogicIT.class);
	private static final String POLL_FLOW_NAME = "triggerFlow";

	private static final String KEY_ID = "Id";
	private static final String KEY_NAME = "Name";
	private static final String KEY_WEBSITE = "Website";
	private static final String KEY_PHONE = "Phone";
	private static final String KEY_NUMBER_OF_EMPLOYEES = "NumberOfEmployees";
//	private static final String KEY_INDUSTRY = "Industry";
	private static final String KEY_CITY = "City";

	private static final int TIMEOUT_SEC = 120;
	private static final String TEMPLATE_NAME = "account-broadcast";

	private SubflowInterceptingChainLifecycleWrapper retrieveAccountFromSalesforceFlow;
	private List<Map<String, Object>> createdAccountsInSiebel = new ArrayList<Map<String, Object>>();
	private BatchTestHelper helper;

	private final Prober pollProber = new PollingProber(TIMEOUT_SEC * 1000, 1000l);
	private final PipelineSynchronizeListener pipelineListener = new PipelineSynchronizeListener(POLL_FLOW_NAME);

	@BeforeClass
	public static void init() {
		System.setProperty("page.size", "1000");
		System.setProperty("polling.frequency", "10000");
		System.setProperty("polling.start.delay", "20000");
		
		// default watermark
		DateTime dt = new DateTime().minusDays(1);
		DateTimeFormatter fmt = DateTimeFormat.forPattern("M/d/y H:m:s");
		String defaultWatermarkString = fmt.print(dt);
		System.setProperty("watermark.default.expression",	defaultWatermarkString);  // one day ago
	}
	
	@Before
	public void setUp() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		registerListeners();
		helper = new BatchTestHelper(muleContext);
	
		// Flow to retrieve accounts from target system after sync in g
		retrieveAccountFromSalesforceFlow = getSubFlow("retrieveAccountFromSalesforceFlow");
		retrieveAccountFromSalesforceFlow.initialise();
		createTestDataInSandBox();
	}

	private void registerListeners() throws NotificationException {
		muleContext.registerListener(pipelineListener);
	}
	
	@After
	public void tearDown() throws Exception {
		stopFlowSchedulers(POLL_FLOW_NAME);
		
		deleteTestAccountsFromSiebel(createdAccountsInSiebel);
		deleteTestAccountsFromSalesforce(createdAccountsInSiebel);
	}

	@Test
	public void testMainFlow() throws Exception {
		// Run poll and wait for it to run
		runSchedulersOnce(POLL_FLOW_NAME);
		waitForPollToRun();
	
		// Wait for the batch job executed by the poll flow to finish
		helper.awaitJobTermination(TIMEOUT_SEC * 1000, 500);
		helper.assertJobWasSuccessful();
	
		Map<String, Object> payload0 = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, createdAccountsInSiebel.get(0));
		Assert.assertNotNull("The account 0 should have been sync but is null", payload0);
		Assert.assertEquals("The account 0 should have been sync (Website)", createdAccountsInSiebel.get(0).get(KEY_WEBSITE), payload0.get(KEY_WEBSITE));
		Assert.assertEquals("The account 0 should have been sync (Phone)", createdAccountsInSiebel.get(0).get(KEY_PHONE), payload0.get(KEY_PHONE));
		Assert.assertEquals("The account 0 should have been sync (Number of Employees)", createdAccountsInSiebel.get(0).get(KEY_NUMBER_OF_EMPLOYEES).toString(), payload0.get(KEY_NUMBER_OF_EMPLOYEES).toString());

		Map<String, Object>  payload1 = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, createdAccountsInSiebel.get(1));
		Assert.assertNotNull("The account 1 should have been sync but is null", payload1);
		Assert.assertEquals("The account 1 should have been sync (Website)", createdAccountsInSiebel.get(1).get(KEY_WEBSITE), payload1.get(KEY_WEBSITE));
		Assert.assertEquals("The account 1 should have been sync (Phone)", createdAccountsInSiebel.get(1).get(KEY_PHONE), payload1.get(KEY_PHONE));
		Assert.assertEquals("The account 1 should have been sync (Number of Employees)", createdAccountsInSiebel.get(1).get(KEY_NUMBER_OF_EMPLOYEES).toString(), payload1.get(KEY_NUMBER_OF_EMPLOYEES).toString());
		
		Map<String, Object>  payload2 = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, createdAccountsInSiebel.get(2));
		Assert.assertNull("The account 2 should have not been sync", payload2);
	}

	private void waitForPollToRun() {
		log.info("Waiting for poll to run ones...");
		pollProber.check(new ListenerProbe(pipelineListener));
		log.info("Poll flow done");
	}
	

	private void createTestDataInSandBox() throws MuleException, Exception {
		// Create object in target system to be updated
		
		String uniqueSuffix = "_" + TEMPLATE_NAME + "_" + UUID.getUUID();
		
		Map<String, Object> salesforceAccount3 = new HashMap<String, Object>();
		salesforceAccount3.put(KEY_NAME, "Name_3_SFDC" + uniqueSuffix);
		salesforceAccount3.put(KEY_WEBSITE, "http://example.com");
		salesforceAccount3.put(KEY_PHONE, "1124567890");
		salesforceAccount3.put(KEY_NUMBER_OF_EMPLOYEES, "6800");
		List<Map<String, Object>> createdAccountInSalesforce = new ArrayList<Map<String, Object>>();
		createdAccountInSalesforce.add(salesforceAccount3);
	
		SubflowInterceptingChainLifecycleWrapper createAccountInSalesforceFlow = getSubFlow("createAccountsInSalesforceFlow");
		createAccountInSalesforceFlow.initialise();
		createAccountInSalesforceFlow.process(getTestEvent(createdAccountInSalesforce, MessageExchangePattern.REQUEST_RESPONSE));
	
		Thread.sleep(1001); // this is here to prevent equal LastModifiedDate
		
		// Create accounts in source system to be or not to be synced
	
		// This account should be synced
		Map<String, Object> siebelAccount0 = new HashMap<String, Object>();
		siebelAccount0.put(KEY_NAME, "Name_0_SIEB" + uniqueSuffix);
		siebelAccount0.put(KEY_WEBSITE, "http://acme.org");
		siebelAccount0.put(KEY_PHONE, "1234567890");
		siebelAccount0.put(KEY_NUMBER_OF_EMPLOYEES, 6000);
		siebelAccount0.put(KEY_CITY, "Las Vegas");
		siebelAccount0.put("Street", "street0A" + uniqueSuffix);
//		siebelAccount0.put(KEY_INDUSTRY, "Education");
		createdAccountsInSiebel.add(siebelAccount0);
				
		// This account should be synced (update)
		Map<String, Object> siebelAccount1 = new HashMap<String, Object>();
		siebelAccount1.put(KEY_NAME,  salesforceAccount3.get(KEY_NAME));
		siebelAccount1.put(KEY_WEBSITE, "http://example.edu");
		siebelAccount1.put(KEY_PHONE, "9114567890");
		siebelAccount1.put(KEY_NUMBER_OF_EMPLOYEES, 7100);
		siebelAccount1.put(KEY_CITY, "Jablonica");
		siebelAccount1.put("Street", "street1A" + uniqueSuffix);
//		siebelAccount1.put(KEY_INDUSTRY, "Government");
		createdAccountsInSiebel.add(siebelAccount1);

		// This account should not be synced because of employees// was: industry
		Map<String, Object> siebelAccount2 = new HashMap<String, Object>();
		siebelAccount2.put(KEY_NAME, "Name_2_SIEB" + uniqueSuffix);
		siebelAccount2.put(KEY_WEBSITE, "http://energy.edu");
		siebelAccount2.put(KEY_PHONE, "3334567890");
		siebelAccount2.put(KEY_NUMBER_OF_EMPLOYEES, 204);
		siebelAccount2.put(KEY_CITY, "London");
		siebelAccount2.put("Street", "street2A" + uniqueSuffix);
//		siebelAccount2.put(KEY_INDUSTRY, "Energetic");
		createdAccountsInSiebel.add(siebelAccount2);

		MuleEvent event = runFlow("createAccountsInSiebelFlow", createdAccountsInSiebel);
		List<?> results = (List<?>) event.getMessage().getPayload();
		
		// assign Siebel-generated IDs
		for (int i = 0; i < createdAccountsInSiebel.size(); i++) {
			createdAccountsInSiebel.get(i).put(KEY_ID, ((CreateResult) results.get(i)).getCreatedObjects().get(0));
		}

		log.info("Results after adding: " + createdAccountsInSiebel.toString());
	}


	@SuppressWarnings("unchecked")
	protected Map<String, Object> invokeRetrieveFlow(SubflowInterceptingChainLifecycleWrapper flow, Map<String, Object> payload) throws Exception {
		MuleEvent event = flow.process(getTestEvent(payload, MessageExchangePattern.REQUEST_RESPONSE));
		Object resultPayload = event.getMessage().getPayload();
		return resultPayload instanceof NullPayload ? null : (Map<String, Object>) resultPayload;
	}
	
	private void deleteTestAccountsFromSiebel(List<Map<String, Object>> createdAccountsInSiebel) throws Exception {
		SubflowInterceptingChainLifecycleWrapper deleteAccountFromSiebelFlow = getSubFlow("deleteAccountsFromSiebelFlow");
		deleteAccountFromSiebelFlow.initialise();
		deleteTestEntityFromSandBox(deleteAccountFromSiebelFlow, createdAccountsInSiebel);
	}

	private void deleteTestAccountsFromSalesforce(List<Map<String, Object>> createdAccountsInA) throws Exception {
		List<Map<String, Object>> createdAccountsInSalesforce = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> c : createdAccountsInA) {
			Map<String, Object> account = invokeRetrieveFlow(retrieveAccountFromSalesforceFlow, c);
			if (account != null) {
				createdAccountsInSalesforce.add(account);
			}
		}
		SubflowInterceptingChainLifecycleWrapper deleteAccountFromSalesforceFlow = getSubFlow("deleteAccountsFromSalesforceFlow");
		deleteAccountFromSalesforceFlow.initialise();
		deleteTestEntityFromSandBox(deleteAccountFromSalesforceFlow, createdAccountsInSalesforce);
	}
	
	private MuleEvent deleteTestEntityFromSandBox(SubflowInterceptingChainLifecycleWrapper deleteFlow, List<Map<String, Object>> entitities) throws Exception {
		List<String> idList = new ArrayList<String>();
		for (Map<String, Object> c : entitities) {
			idList.add(c.get(KEY_ID).toString());
		}
		return deleteFlow.process(getTestEvent(idList, MessageExchangePattern.REQUEST_RESPONSE));
	}

}
