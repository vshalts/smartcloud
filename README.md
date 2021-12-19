# interview

Setup is the same as described in smartcloud-prices README.  Run as usual with `sbt`

Assumptions and design decisions:

1. Correct real-time prices are more important than availability here
(assumption done for simplicity, caching and rate-limiting can be used 
to overcome this limitation, but it increases complexity).

2. One instance of the service will be enough.

3. Common logic can be extracted into a common service - SmartcloudService.
The assumption is that SmartcloudPriceService and SmartcloudInstanceKindService
can provide some high-level functionality (like rate limiting, caching, data 
transformation, etc) while SmartcloudService is responsible only for HTTP retrieving.

4. Error handling is simplified to bare minimum and there only to show that it exists. 

5. No logging, no tests, etc.
