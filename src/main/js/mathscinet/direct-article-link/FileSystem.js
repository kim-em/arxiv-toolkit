var fileSystem;

// Setup a file system
window.requestFileSystem  = window.requestFileSystem || window.webkitRequestFileSystem;
window.webkitStorageInfo.requestQuota(PERSISTENT, 1024*1024, function(grantedBytes) {
                                      window.requestFileSystem(PERSISTENT, grantedBytes, function(fs) { console.log("Successfully requested chrome file system."); fileSystem = fs; }, function(error) { console.log("Failed to create file system."); });
                                      }, function(e) {
                                      console.log('Error', e);
                                      });

function findFilesByName(predicate, callback) {
    function errorHandler(error) { console.log("An error occurred while reading file names from the chrome file system: ", error); }
    function toArray(list) {
        return Array.prototype.slice.call(list || [], 0);
    }
    
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
    }
}

