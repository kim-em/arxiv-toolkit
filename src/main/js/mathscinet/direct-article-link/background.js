// Called when the url of a tab changes.
function checkForMathSciNet(tabId, changeInfo, tab) {
 if (tab.url.indexOf('mathscinet') > -1) {
	    // ... show the page action.
	    chrome.pageAction.show(tabId);
   }
 };

// Listen for any changes to the URL of any tab.
chrome.tabs.onUpdated.addListener(checkForMathSciNet);

var dropboxClient
var dropboxClientStarting = false;

function startDropboxClient() {
  if(typeof dropboxClient === "undefined") {
    dropboxClientStarting = true;
    console.log("Starting dropbox client.");
    var client = new Dropbox.Client({ key: "cIrBuCz5CWA=|fGPZmdP8KEuRpnB0DUK27/oCcPvCWXzzJAF16wpHuA==" /* encoded at https://dl-web.dropbox.com/spa/pjlfdak1tmznswp/api_keys.js/public/index.html */, sandbox: true });
    client.authDriver(new Dropbox.Drivers.Chrome({ receiverPath: "oauth/chrome_oauth_receiver.html" }));
    client.authenticate(function(error, client) {
     if (error) {
       console.log("Dropbox authentication failed: ", error);
       return false;
     } else {
       console.log("Successfully authenticated Dropbox!");
       dropboxClient = client;
     }
   });
  }
}

function waitForDropboxClient(callback) {
  if(dropboxClientStarting) {
    if(typeof dropboxClient === "undefined") {
      console.log("waiting on dropbox client");
      setTimeout(function() { waitForDropboxClient(callback); }, 500);
    } else {
      callback();
    }
  } else {
    startDropboxClient();
    waitForDropboxClient(callback);
  }
}

function saveToDropbox(metadata) {
  waitForDropboxClient(function() {
    console.log("Writing file to dropbox.");
    dropboxClient.writeFile(metadata.filename, metadata.blob);
    console.log("Finished writing file to dropbox.");
  });
}

function dataURItoBlob(dataURI) {
    // convert base64 to raw binary data held in a string
    // doesn't handle URLEncoded DataURIs - see SO answer #6850276 for code that does this
    var byteString = atob(dataURI.split(',')[1]);
    console.log("... byteString.length: " + byteString.length);

    // separate out the mime component
    var mimeString = dataURI.split(',')[0].split(':')[1].split(';')[0]
    console.log("... mimeString: " + mimeString);

    // write the bytes of the string to an ArrayBuffer
    var ab = new ArrayBuffer(byteString.length);
    var ia = new Uint8Array(ab);
    for (var i = 0; i < byteString.length; i++) {
      ia[i] = byteString.charCodeAt(i);
    }

    // write the ArrayBuffer to a blob, and you're done
    return new Blob([ab], {type: mimeString});
  }

  function unpackMetadata(metadata) {
    if(metadata.uri) {
      metadata.blob = dataURItoBlob(metadata.uri);
      delete metadata['uri'];
      return metadata;
    } else {
      console.log("Warning: trying to unpack metadata without a uri key!");
      return metadata
    }
  }

  chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
      console.log("Background page received a " + request.cmd + " request.")
      if (request.cmd == "saveToDropbox") {
        setTimeout(function() { saveToDropbox(unpackMetadata(request.metadata)) }, 0);
      }
    });