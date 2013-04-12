var fileSystem;

var fileSystemInitializing = true;

// FIXME we should do this once and for all, not per page!

// If only we were an app, we can ask for permission for "syncFileSystem", and then use chrome.syncFileSystem.requestFileSystem  here.

// Setup a file system
window.requestFileSystem  = window.requestFileSystem || window.webkitRequestFileSystem;
window.webkitStorageInfo.requestQuota(PERSISTENT, 1024*1024*1024, function(grantedBytes) {
  window.requestFileSystem(
    PERSISTENT, 
    grantedBytes, 
    function(fs) { 
      console.log("Successfully requested chrome file system."); 
      fileSystemInitializing = false;
      fileSystem = fs; 
    }, 
    function(error) { 
      console.log("Failed to create file system."); 
      fileSystemInitializing = false;
    });
}, function(e) {
  console.log('Error', e);
  fileSystemInitializing = false;
});

function findFilesByName(predicate, callback) {
  function errorHandler(error) { console.log("An error occurred while reading file names from the chrome file system: ", error); }
  function toArray(list) {
    return Array.prototype.slice.call(list || [], 0);
  }

  if(fileSystemInitializing) {
    console.log("warning: chrome filesystem not yet available, sleeping...");
    setTimeout(function() { findFilesByName(predicate, callback) }, 500);
  } else {
    if(typeof fileSystem !== "undefined") {
      var dirReader = fileSystem.root.createReader();
      var entries = [];

      // Call the reader.readEntries() until no more results are returned.
      var readEntries = function() {
        dirReader.readEntries (function(results) {
         if (!results.length) {
           callback(entries.sort());
         } else {
           entries = entries.concat(toArray(results).filter(function(entry) { return predicate(entry.name); }));
           readEntries();
         }
       }, errorHandler);
      };

        readEntries(); // Start reading dirs.
      } else {
        console.log("Warning: chrome filesystem not available!");
        callback([]);
      }
    }
  }

  function deleteFile(name) {
    fileSystem.root.getFile(name, {create: false}, function(fileEntry) {
      fileEntry.remove(function() {
        console.log('File removed.');
      }, function() {});
    });
  }