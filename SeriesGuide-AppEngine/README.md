SeriesGuide App Engine
======================

Deployment strategy
-------------------
Taken from [Google][1].

* When you want to introduce an incremental, but non-breaking change, keep the API version constant and deploy over the existing API.
* When you introduce a breaking change to your API, increment the API version.
* For additional protection, increment the App Engine app version as well and then deploy the new API version to that new App Engine app version. This lets you use the built-in flexibility of App Engine to quickly switch between App Engine versions and serve from the old working versions if you run into unexpected problems.


 [1]: https://developers.google.com/appengine/docs/java/endpoints/test_deploy#managing_your_backend_api_versions