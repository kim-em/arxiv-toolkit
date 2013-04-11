function main() {
    // Setup a file system
    window.requestFileSystem  = window.requestFileSystem || window.webkitRequestFileSystem;
    window.webkitStorageInfo.requestQuota(PERSISTENT, 1024*1024, function(grantedBytes) {
                                          window.requestFileSystem(PERSISTENT, grantedBytes, function(fs) { console.log("Successfully requested chrome file system."); fileSystem = fs; }, function(error) { console.log("Failed to create file system."); });
                                          }, function(e) {
                                            console.log('Error', e);
                                          });
        
    rewriteArticleLinks();
/* Hmm... my attemps to integrate with Dropbox seem to have run into a dead-end. The authenticate call never reaches the callback.
   There are some error messages on the console about permissions to "tabs" and "experimental.identity", which I haven't been able to resolve. */
//    var client = new Dropbox.Client({ key: "cIrBuCz5CWA=|fGPZmdP8KEuRpnB0DUK27/oCcPvCWXzzJAF16wpHuA==" /* encoded at https://dl-web.dropbox.com/spa/pjlfdak1tmznswp/api_keys.js/public/index.html */, sandbox: true });
//    client.authDriver(new Dropbox.Drivers.Chrome({ receiverPath: chrome.extension.getURL("oauth/chrome_oauth_receiver.html") }));
//    client.authenticate(function(error, client) {
//                        if (error) {
//                            alert("Dropbox authentication failed: ", error);
//                            // Don't forget to return from the callback, so you don't execute the code
//                            // that assumes everything went well.
//                            return false;
//                        } else {
//                            alert("Successfully authenticated Dropbox!");
//                        }
//                        });
}

var fileSystem;

function loadBlob(url, callback) {
    if(typeof callback === "undefined") callback = function() {};
    var xhr = new XMLHttpRequest();
    xhr.open('GET', url, true);
    
    xhr.responseType = 'arraybuffer';
    
    xhr.onload = function(e) {
        if (this.status == 200) {
            var blob = new Blob([this.response], {type: "application/pdf"});
            
            callback(blob)
        }
    };
    
    xhr.send();
}

function forkCallback(callbacks) {
    return function(response) {
        callbacks.forEach(function(callback) { callback(response); } );
    }
}

function blob2Text(blob, callback) {
    var f = new FileReader();
    f.onload = function(e) {
        callback(e.target.result)
    }
    f.readAsText(blob);
}

function processPDFLink(url) {
    findFilesByName(function(name) { return name.indexOf(PDFFileName()) !== -1; }, function(entries) { console.log("Found " + entries.length + " files in the file system."); });
    loadBlob(url, function(blob) { verifyBlob(blob, forkCallback([ saveBlobToFile, saveBlobToFileSystem, showBlobInIFrame ]), indicateNoPDF) });
}

function verifyBlob(blob, success, failure) {
    blob2Text(blob.slice(0, 10), function(text) {
              if(text.indexOf("%PDF") !== -1) {
                console.log("Successfully loaded PDF blob!");
                success(blob);
              } else {
                console.log("Loaded blob, but it didn't look like a PDF");
                failure(blob);
              }
              });    
}

function saveBlobToFile(blob) {
    window.saveAs(blob, PDFFileName());
}

function saveBlobToFileSystem(blob) {
    function errorHandler(error) { console.log("An error occurred while saving to the chrome file system: ", error); }
    
    if(typeof fileSystem !== "undefined") {
        fileSystem.root.getFile(PDFFileName(), {create: true}, function(fileEntry) {
            // Create a FileWriter object for our FileEntry .
            fileEntry.createWriter(function(fileWriter) {
                       fileWriter.onwriteend = function(e) {
                            console.log('Write completed.');
                       };
                       fileWriter.onerror = function(e) {
                            console.log('Write failed: ' + e.toString());
                       };
                       fileWriter.write(blob);
                   }, errorHandler);
            console.log("Writing blob to " + fileEntry.toURL());
        }, errorHandler);
    }
}

function showBlobInIFrame(blob) {
    var iframe = $('<iframe/>').attr({id: 'pdf-iframe', src:window.URL.createObjectURL(blob), width:'100%', height: $(window).height(), border:'none' }).appendTo('div#content');
    $("#loading").text('✔');
}
function indicateNoPDF() {
    $("#loading").text('✘');
}

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

function MRNumber() {
    return "MR" + $("div.headlineMenu a:contains('Make Link')").attr('href').replace("http://www.ams.org/mathscinet-getitem?mr=", "");
}

function PDFFileName() {
    return MRNumber() + ".pdf";
}

function findPDFURL(metadata, callback) {
    if(metadata.MRNUMBER) {
        // check the local filesystem
    }
    // TODO move stuff here
}

function rewriteArticleLinks() {
    var elements = $("a:contains('Article'), a:contains('Chapter'), a:contains('Thesis'), a:contains('Book')");
    elements.attr('href', function() { return this.href.replace(/http:\/\/[^\/]*\/leavingmsn\?url=/,""); });
    var eventually = function(link) { };
    if(elements.length == 1) {
        eventually = function(link) {
            if(link.href.indexOf("pdf") !== -1 || link.href.indexOf("displayFulltext") !== -1 /* CUP */) {
                $("a:contains('Article'), a:contains('Chapter'), a:contains('Thesis'), a:contains('Book')").after($('<span/>').attr({id: 'loading'}).text('…'))
                processPDFLink(link.href);
            }
        }
    }
    elements.each(function() {
            if(this.href.startsWith("http://dx.doi.org/10.1006") || this.href.startsWith("http://dx.doi.org/10.1016")) {
              // handle Elsevier separately
              var link = this;
              loadAsync(this.href, function(response) {
                        var regex = /pdfurl="([^"]*)"/;
                        link.href = regex.exec(response)[1];
                        eventually(link);
                });
            } else if(this.href.startsWith("http://dx.doi.org/10.1017/S") || this.href.startsWith("http://dx.doi.org/10.1051/S") || this.href.startsWith("http://dx.doi.org/10.1112/S0010437X") || this.href.startsWith("http://dx.doi.org/10.1112/S14611570") || this.href.startsWith("http://dx.doi.org/10.1112/S00255793")) {
                // Cambridge University Press
                var link = this;
                loadAsync(this.href, function(response) {
                    var regex = /<a href="([^"]*)"\s*title="View PDF" class="article-pdf">/;
                    link.href = "http://journals.cambridge.org/action/" + regex.exec(response)[1].trim();
                    eventually(link);
                });
            } else if(this.href.startsWith("http://dx.doi.org/10.1002/")) {
                // Wiley
               var link = this;
               loadAsync("http://onlinelibrary.wiley.com/doi/" + this.href.slice(18) + "/pdf", function(response) {
                        var regex = /id="pdfDocument" src="([^"]*)/;
                        link.href = regex.exec(response)[1];
                        eventually(link);
               });
            } else {
                if(this.href.startsWith("http://dx.doi.org/")) {
                    var link = this;
                    loadJSON(
                        this.href.replace("http://dx.doi.org/", "http://evening-headland-2959.herokuapp.com/"),
                             function (data) { if(data.redirect) link.href = data.redirect; eventually(link); }
                    );
                };
                if(this.href.startsWith("http://projecteuclid.org/getRecord?id=")) {
                    this.href = this.href.replace("http://projecteuclid.org/getRecord?id=", "http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle=");
                    eventually(this);
                }
                if(this.href.startsWith("http://www.numdam.org/item?id=")) {
                      this.href = this.href.replace("http://www.numdam.org/item?id=", "http://archive.numdam.org/article/") + ".pdf";
                      eventually(this);
                };
            }
        });

}



if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str){
        return this.slice(0, str.length) == str;
    };
}

function loadAsync(url, callback) {
    if(typeof callback === "undefined") callback = function() {}
        if(typeof GM_xmlhttpRequest === "undefined") {
            // console.log("GM_xmlhttpRequest unavailable")
            // hope for the best (i.e. that we're running as a Chrome extension)
            $.get(url, callback)
        } else {
            // console.log("GM_xmlhttpRequest available")
            // load via GM
            GM_xmlhttpRequest({
                              method: "GET",
                              url: url,
                              onload: function(response) {
                              callback(response.responseText);
                              }
                              });
        }
}

function loadJSON(url, callback) {
    if(typeof callback === "undefined") callback = function() {}
        var request = {
        method: "GET",
        url: url,
        dataType: "json",
        success: callback
        }
    if(typeof GM_xmlhttpRequest === "undefined") {
        // console.log("GM_xmlhttpRequest unavailable")
        // hope for the best (i.e. that we're running as a Chrome extension)
        $.ajax(request)
    } else {
        // console.log("GM_xmlhttpRequest available")
        // load via GM
        GM_xmlhttpRequest(request);
    }
}

main()
