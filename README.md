[![Build Status](https://travis-ci.org/nytimes/Register.svg?branch=master)](https://travis-ci.org/nytimes/Register)

![Register Logo](https://github.com/nytm/register/blob/master/images/register-logo.png?raw=true)

Register is an Android library for easier testing of Google Play's In-app Billing.


[Blog Post](https://open.nytimes.com/register-better-in-app-billing-testing-on-android-73af5fcc36dc)

### The Problems:

+ Google Play Billing implementations on Android are hard to get right
+ When payments are involved, developers sleep better having a way to test their functionality prior to release
+ Before an app is promoted to Alpha in the Play Store, we do not have an offical way to test payments

The New York Times Android Team developed a fake implementation of Google Play Billing called Register, which can be used as a companion app for testing Play Billing purchases and subscriptions. 

Similar to a mock web server, you can point your app to use Register rather than the real Play Store In-app Billing implementation. Using Register, you'll be able to validate in-advance whether your purchasing flows work correctly.

Register has been used to test purchasing flows of our [Flagship Reader App](https://play.google.com/store/apps/details?id=com.nytimes.android&hl=en) and [NYT Crosswords App](https://play.google.com/store/apps/details?id=com.nytimes.crossword&hl=en) for three years and counting.

![Register Sample](https://github.com/nytm/register/blob/master/images/registerCompanion.png?raw=true)

### Overview

Register is a Companion App. and library that allows seamless mocking of responses from Google Play Store Billing. 
Register works by implementing the same interface as the Google Play Store Billing library [Google Play Billing](https://developer.android.com/google/play/billing/billing_overview).

From a client's perspective, there is no difference in how you work with Google Play Billing or Register's implementation.

If you've used [Amazon's IAP Tester Utility](https://developer.amazon.com/public/apis/earn/in-app-purchasing/docs-v2/testing-iap), 
you'll find Register's workflow to be very familiar.

### Using Register

**Step 0:** Register needs a configuration file that declares the mock purchases, subscriptions and users that you will be testing against.  

Here's a sample that we use at NYTimes. The format needs to be the same as below when creating your own fake purchases. This JSON file (`register.json`) should be included in the assets folder of your module.
```json
{
	"skus": {
	    "register.sample.iap": {
			"type": "inapp",
			"price" : "1.00",
			"title" : "Sample In App Purchase Item",
			"description" : "This is an in app purchase item for use with Register sample app",
			"package" : "com.nytimes.android.external.register"
	    },
		"register.sample.sub": {
			"type": "subs",
			"price" : "10.00",
			"title" : "Sample Subscription Item1",
			"description" : "This is a subscription item for use with Register sample app",
			"package" : "com.nytimes.android.external.register"
		}
	},
	"users": [
		"user1@register.nytimes.com",
		"user2@register.nytimes.com"
	]
}

```
For Register to find the aforementioned configuration file (in case you change name or location), you need to change the data contained in the `configuration\configuration.properties` file:
```
ide.json.path=subfolder/from/root
file.json.name=newFileName.json
```

**Step 1:** Add Register as a dependency to your client app:
```groovy 
compile 'com.nytimes.android:register:0.0.8'
```

or, on Android Gradle Plugin 3.0 or later:

```groovy 
implementation 'com.nytimes.android:register:0.0.8'
```

**Step 2:** Create a test Google Billing Client (or a real client):

```java
 private void initGooglePlayBillingClient() {
        if (prefsManager.isUsingTestBillingClient()) {
            googleBillingClient = new BillingClientTesting(...);
        } else {
            googleBillingClient = new BillingClient(...);
        }
    }
```

**Step 3:** Make a purchase, similar to how you would with the regular In-app Billing API.

![Register Sample](https://github.com/nytm/register/blob/master/images/purchase.png?raw=true)

**Step 4:** Go to the companion app to view the purchase.

![Register Sample](https://github.com/nytm/register/blob/master/images/purchased.png?raw=true)


### Fully Configurable (Configuration App)

Register's companion app allows you to view both successful and unsuccessful purchases directly on your Android device. Additionally, you can control responses back to your client app for values such as `getSkuDetails`.

See the image below for all configurable options on a response.

![Register Sample](https://github.com/nytm/register/blob/master/images/registerCompanion.png?raw=true)

### Sample App

**SampleApp** is a client app that showcases working with Register. See [SampleActivity](https://github.com/nytimes/Register/blob/master/sampleApp/src/main/java/com/nytimes/android/external/register/sample/SampleActivity.kt) for a demo of the purchasing flow.

### Gradle

**For Android Gradle Plugin 3.0**

```groovy 
implementation 'com.nytimes.android:register:0.0.8'
```

**For projects using older versions of the plugin**

```groovy
compile 'com.nytimes.android:register:0.0.8'
```
