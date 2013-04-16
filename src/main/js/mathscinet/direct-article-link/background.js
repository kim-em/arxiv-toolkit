// Called when the url of a tab changes.
function checkForMathSciNet(tabId, changeInfo, tab) {
	  if (tab.url.indexOf('mathscinet') > -1) {
	    // ... show the page action.
	    chrome.pageAction.show(tabId);
	}
};

// Listen for any changes to the URL of any tab.
chrome.tabs.onUpdated.addListener(checkForMathSciNet);

/* Hmm... my attemps to integrate with Dropbox seem to have run into a dead-end. The authenticate call never reaches the callback.
There are some error messages on the console about permissions to "tabs" and "experimental.identity", which I haven't been able to resolve. */
   var client = new Dropbox.Client({ key: "cIrBuCz5CWA=|fGPZmdP8KEuRpnB0DUK27/oCcPvCWXzzJAF16wpHuA==" /* encoded at https://dl-web.dropbox.com/spa/pjlfdak1tmznswp/api_keys.js/public/index.html */, sandbox: true });
   client.authDriver(new Dropbox.Drivers.Chrome({ receiverPath: "oauth/chrome_oauth_receiver.html" }));
   client.authenticate(function(error, client) {
                       if (error) {
                           alert("Dropbox authentication failed: ", error);
                           // Don't forget to return from the callback, so you don't execute the code
                           // that assumes everything went well.
                           return false;
                       } else {
                           alert("Successfully authenticated Dropbox!");
                       }
                       });
