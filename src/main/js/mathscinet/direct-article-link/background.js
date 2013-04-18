// These keys also appear in Settings.js
var settingsKeys = ["#berserk","#inline","#filename", "#download","#download-zip", "#store","#store-synchronized","#dropbox","#drive","#mega","#mega-account"];
var settings = {};

chrome.storage.sync.get(settingsKeys, function(items) {
  settings = items;
});


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
var papersSavedInDropbox = {}; // a map, MRNUMBERs to filenames

function startDropboxClient() {
  if(settings["#dropbox"]) {
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
         client.readdir("", null, function(status, filenames) {
          console.log("Dropbox reports " + filenames.length + " total files.");
          var regex = /MR[0-9]*/;
          for (var i = 0; i < filenames.length; i++) {
            var match = regex.exec(filenames[i]);
            // console.log(match + " --> "  + filenames[i]);
            if(match !== null) {
              papersSavedInDropbox[match[0]] = filenames[i];
            }
          }
          dropboxClient = client;
        });
       }
     });
    }
  } else {
    log.console("Warning, someone tried to start the dropbox client, but dropbox isn't turned on.");
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
    setTimeout(function() { waitForDropboxClient(callback); }, 500);
  }
}

function saveToDropbox(metadata) {
  if(settings["#dropbox"]) {
    if(papersSavedInDropbox[metadata.MRNUMBER]) {
      console.log("Ignored 'saveToDropbox' command, it looks like the file is already there.");
    } else {
      waitForDropboxClient(function() {
        console.log("Writing file to dropbox.");
        dropboxClient.writeFile(metadata.filename, metadata.blob);
        papersSavedInDropbox[metadata.MRNUMBER] = metadata.filename;
        console.log("Finished writing file to dropbox.");
      });
    }
  } else {
    console.log("Ignoring 'saveToDropbox' command.");
  }
}

function loadFromDropbox(metadata, callback, onerror) {
  console.log("loadingFromDropbox...");
  // console.log("... requested filename " + metadata.filename);
  // console.log("... local cache: " + papersSavedInDropbox[metadata.MRNUMBER]);

  metadata.filename = papersSavedInDropbox[metadata.MRNUMBER] || metadata.filename;

  if(!metadata.filename) {
    dropboxClient.findByName("", metadata.MRNUMBER, null, function(status, results) {
      if(results.length > 0) {
        metadata.filename = results[0].name;
        continuation();  
      } else {
        onerror();
      }
    });
  } else {
    continuation();
  }

  function continuation() {
    waitForDropboxClient(function() {
      console.log("Reading file from dropbox.");
      dropboxClient.readFile(metadata.filename, { blob: true }, function(status, blob) {
        if(status == null) {
          console.log("Finished reading file from dropbox.");
          readAsDataURL(blob, function(uri) {
            metadata.uri = uri;
            callback(metadata);
          });
        } else {
          console.log("Error while reading file from dropbox.");
          onerror();
        }
      });
    });
  }
}

var euclidHandles = {};

function attachHandle(metadata) {
  if(metadata.handle) {
    // console.log("Current state of euclidHandles: " + JSON.stringify(euclidHandles));
    var newMetadata = euclidHandles[metadata.handle];
    newMetadata.blob = metadata.blob;
    if(newMetadata.responseCallback) {
      var callback = newMetadata.responseCallback;
      delete newMetadata.responseCallback;
      euclidHandles[metadata.handle] = newMetadata;
      packMetadata(newMetadata, function(packedMetadata) { callback(packedMetadata); });
    }
    return newMetadata;
  } else {
    return metadata;
  }
}

chrome.runtime.onMessage.addListener(
  function(request, sender, sendResponse) {
    console.log("Background page received a '" + request.cmd + "' request.")
    if (request.cmd == "saveToDropbox") {
      setTimeout(function() { saveToDropbox(attachHandle(unpackMetadata(request.metadata))); }, 0);
    } else if(request.cmd == "listPapersSavedInDropbox") {
      waitForDropboxClient(function() {
        console.log("... sending response for 'listPapersSavedInDropbox' request.");
        var response = { papersSavedInDropbox: papersSavedInDropbox };
        sendResponse(response);
      });
      return true; // we're sending a delayed response
    } else if(request.cmd == "loadFromDropbox") {
      loadFromDropbox(request.metadata, function(responseMetadata) {
        sendResponse(responseMetadata);
      }, function() {
        /* TODO handle failure */
      });
      return true; // we're sending a delayed response
    } else if(request.cmd == "mentionEuclidHandle") {
      request.metadata.responseCallback = sendResponse;
      euclidHandles[request.handle] = request.metadata;
      // console.log("Current state of euclidHandles: " + JSON.stringify(euclidHandles));
      return true;
    }
  });