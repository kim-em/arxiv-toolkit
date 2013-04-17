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
        var regex = /MR[0-9]*/;
        for (var i = 0; i < filenames.length; i++) {
          /* make this more robust; what if there's no MRNUMBER? */
          papersSavedInDropbox[regex.exec(filenames[i])[0]] = filenames[i];
    }
       });
       dropboxClient = client;
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
    waitForDropboxClient(callback);
  }
}

function saveToDropbox(metadata) {
  if(settings["#dropbox"]) {
  waitForDropboxClient(function() {
    console.log("Writing file to dropbox.");
    dropboxClient.writeFile(metadata.filename, metadata.blob);
    console.log("Finished writing file to dropbox.");
  });
} else {
  console.log("Ignoring 'saveToDropbox' command.");
}
}

function loadFromDropbox(metadata, callback, onerror) {
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
    var newMetadata = euclideHandles[metadata.handle];
    newMetadata.blob = metadata.blob;
    return newMetadata;
  } else {
    return metadata;
  }
}

  chrome.runtime.onMessage.addListener(
    function(request, sender, sendResponse) {
      console.log("Background page received a '" + request.cmd + "'' request.")
      if (request.cmd == "saveToDropbox") {
        setTimeout(function() { saveToDropbox(attachHandle(unpackMetadata(request.metadata)) }, 0);
      } else if(request.cmd == "listPapersSavedInDropbox") {
        sendResponse(papersSavedInDropbox)
      } else if(request.cmd == "loadFromDropbox") {
        loadFromDropbox(request.metadata, function(responseMetadata) {
          sendResponse(responseMetadata);
        }, function() {
          /* TODO handle failure */
        });
      } else if(request.cmd == "mentionEuclidHandle") {
        euclidHandles[request.handle] = request.metadata;
      }
    });