
# Anypoint Template: Siebel to Salesforce Account Broadcast	

<!-- Header (start) -->
Broadcasts changes to accounts in Siebel to Salesforce in real time. The detection criteria, and fields to move are configurable. Additional systems can be added to be notified of the changes. Real time synchronization is achieved via rapid polling of Siebel. 

This template uses watermarking to ensure that only the most recent items are synchronized and batch to efficiently process many records at a time.

![44e579a8-7ea2-48f3-bd4c-ff63890b7cb3-image.png](https://exchange2-file-upload-service-kprod.s3.us-east-1.amazonaws.com:443/44e579a8-7ea2-48f3-bd4c-ff63890b7cb3-image.png)
<!-- Header (end) -->

# License Agreement
This template is subject to the conditions of the <a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>. Review the terms of the license before downloading and using this template. You can use this template for free with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio. 
# Use Case
<!-- Use Case (start) -->
This Anypoint template serves as a foundation for setting an online sync of accounts from Oracle Siebel Business Objects to Salesforce.
Every time there is a new account or a change in an already existing one, the template polls for changes from Oracle Siebel Business Objects source instance and updates the account in Salesforce target instance.

Requirements have been set not only to be used as examples, but also to establish a starting point to adapt your integration to your requirements.

As implemented, this template leverages the Mule batch module.

The batch job is divided in *Process* and *On Complete* stages.

The integration is triggered by a scheduler defined in the flow. The template then makes a query for the newest Oracle Siebel Business Objects updates or creates matching a filtering criteria and executes the batch job.
During the *Process* stage, each Siebel Account is filtered depending on existing matching Account in Salesforce.
The last step of the *Process* stage groups the users and creates or updates them in Salesforce.
Finally during the *On Complete* stage the template logs statistics data to the console.
<!-- Use Case (end) -->

# Considerations
<!-- Default Considerations (start) -->

<!-- Default Considerations (end) -->

<!-- Considerations (start) -->
To make this template run, there are certain preconditions that must be considered. All of them deal with the preparations in both source and destination systems, that must be made for the template to run smoothly. Failing to do so can lead to unexpected behavior of the template.
<!-- Considerations (end) -->

## Salesforce Considerations

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>.
- How can I modify the Field Access Settings? See: [Salesforce: Modifying Field Access Settings](https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US "Salesforce: Modifying Field Access Settings")


### As a Data Destination

There are no considerations with using Salesforce as a data destination.
## Oracle Siebel Considerations

This template uses date time or timestamp fields from Oracle Siebel to do comparisons and take further actions. While the template handles the time zone by sending all such fields in a neutral time zone, it cannot discover the time zone in which the Siebel instance is on. It's up to you to provide this information. See [Oracle's Setting Time Zone Preferences](http://docs.oracle.com/cd/B40099_02/books/Fundamentals/Fund_settingoptions3.html).

### As a Data Source

To make the Siebel connector work smoothly you have to provide the correct version of the Siebel JAR files that work with your Siebel installation. [See more](https://docs.mulesoft.com/connectors/siebel-connector#prerequisites).

# Run it!
Simple steps to get this template running.
<!-- Run it (start) -->

<!-- Run it (end) -->

## Running On Premises
In this section we help you run this template on your computer.
<!-- Running on premise (start) -->

<!-- Running on premise (end) -->

### Where to Download Anypoint Studio and the Mule Runtime
If you are new to Mule, download this software:

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)

**Note:** Anypoint Studio requires JDK 8.
<!-- Where to download (start) -->

<!-- Where to download (end) -->

### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your Anypoint Platform credentials, search for the template, and click Open.
<!-- Importing into Studio (start) -->

<!-- Importing into Studio (end) -->

### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

1. Locate the properties file `mule.dev.properties`, in src/main/resources.
2. Complete all the properties required per the examples in the "Properties to Configure" section.
3. Right click the template project folder.
4. Hover your mouse over `Run as`.
5. Click `Mule Application (configure)`.
6. Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`.
7. Click `Run`.

<!-- Running on Studio (start) -->

<!-- Running on Studio (end) -->

### Running on Mule Standalone
Update the properties in one of the property files, for example in mule.prod.properties, and run your app with a corresponding environment variable. In this example, use `mule.env=prod`. 


## Running on CloudHub
When creating your application in CloudHub, go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the mule.env value.
<!-- Running on Cloudhub (start) -->
Once your app is all set and started, there is no need to do anything else. Every time an account is created or modified, it  automatically synchronizes to Salesforce by the account name.
<!-- Running on Cloudhub (end) -->

### Deploying a Template in CloudHub
In Studio, right click your project name in Package Explorer and select Anypoint Platform > Deploy on CloudHub.
<!-- Deploying on Cloudhub (start) -->

<!-- Deploying on Cloudhub (end) -->

## Properties to Configure
To use this template, configure properties such as credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
<!-- Application Configuration (start) -->

- scheduler.frequency `60000`
- scheduler.start.delay `0`
- watermark.default.expression `"2018-12-13T03:00:59Z"`
- page.size `200`

**Oracle Siebel Business Objects Connector Configuration**

- sieb.user `SADMIN`
- sieb.password `SADMIN`
- sieb.server `192.168.10.8`
- sieb.serverName `SBA_82`
- sieb.objectManager `EAIObjMgr_enu`
- sieb.port `2321`

**Salesforce Connector Configuration**

- sfdc.username `bob.dylan@org`
- sfdc.password `DylanPassword123`
- sfdc.securityToken `avsfwCUl7apQs56Xq2AKi3X`
<!-- Application Configuration (end) -->

# API Calls
<!-- API Calls (start) -->
Salesforce imposes limits on the number of API calls that can be made. Therefore calculating this amount may be an important factor to consider. The template calls to the API can be calculated using the formula:

- ***X + X / ${page.size}*** -- Where ***X*** is the number of accounts to synchronize on each run. 
- Divide by ***${page.size}*** because by default, accounts are gathered in groups of ${page.size} for each upsert API call in the aggregation step. Also consider that these calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from the original instance, then 11 API calls are made (10 + 1).
<!-- API Calls (end) -->

# Customize It!
This brief guide provides a high level understanding of how this template is built and how you can change it according to your needs. As Mule applications are based on XML files, this page describes the XML files used with this template. More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml
<!-- Customize it (start) -->

<!-- Customize it (end) -->

## config.xml
<!-- Default Config XML (start) -->
This file provides the configuration for connectors and configuration properties. Only change this file to make core changes to the connector processing logic. Otherwise, all parameters that can be modified should instead be in a properties file, which is the recommended place to make changes.
<!-- Default Config XML (end) -->

<!-- Config XML (start) -->

<!-- Config XML (end) -->

## businessLogic.xml
<!-- Default Business Logic XML (start) -->
This file holds the functional aspect of the template, directed by a flow responsible for conducting the business logic.

The functional aspect of the template is implemented in this XML file, directed by a flow that polls for SalesForce creates or updates.
Several message processors constitute four high level actions that fully implement the logic of this template:

1. The template queries all the existing accounts from Oracle Siebel Business Objects created or modified after a watermark.
2. During the *Process* stage, each Siebel account is filtered depending on existing matching account in Salesforce.
3. The last step of the *Process* stage groups the users, creates or updates them in Salesforce.
4. Finally during the *On Complete* stage the template logs statistics data to the console.
<!-- Default Business Logic XML (end) -->

<!-- Business Logic XML (start) -->

<!-- Business Logic XML (end) -->

## endpoints.xml
<!-- Default Endpoints XML (start) -->
This file is formed by a flow containing the Scheduler that periodically query Siebel for updated or created accounts that meet the defined criteria in the query, and executes the batch job process with the query results.
<!-- Default Endpoints XML (end) -->

<!-- Endpoints XML (start) -->

<!-- Endpoints XML (end) -->

## errorHandling.xml
<!-- Default Error Handling XML (start) -->
This file handles how your integration reacts depending on the different exceptions. This file provides error handling that is referenced by the main flow in the business logic.
<!-- Default Error Handling XML (end) -->

<!-- Error Handling XML (start) -->

<!-- Error Handling XML (end) -->

<!-- Extras (start) -->

<!-- Extras (end) -->
