// Called when the url of a tab changes.
function checkForMathSciNet(tabId, changeInfo, tab) {
	  if (tab.url.indexOf('mathscinet') > -1) {
	    // ... show the page action.
	    chrome.pageAction.show(tabId);
	}
};

// Listen for any changes to the URL of any tab.
chrome.tabs.onUpdated.addListener(checkForMathSciNet);