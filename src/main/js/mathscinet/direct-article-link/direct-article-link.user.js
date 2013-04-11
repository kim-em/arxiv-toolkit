function main() {    
    console.log("direct-article-link.user.js starting up on " + location.href + " at " + new Date().getTime());
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

function processPDFLink(url) {
    findFilesByName(function(name) { return name.indexOf(PDFFileName()) !== -1; }, function(entries) { console.log("Found " + entries.length + " files in the file system."); });
    // TODO if we found a file, use that instead!
    loadBlob(url, function(blob) { verifyBlob(blob, forkCallback([ saveBlobToFile, saveBlobToFileSystem, showBlobInIFrame ]), indicateNoPDF) });
}

function forkCallback(callbacks) {
    return function(response) {
        callbacks.forEach(function(callback) { callback(response); } );
    }
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
    showInIFrame(window.URL.createObjectURL(blob));
}

function showInIFrame(url) {
    var iframe = $('<iframe/>').attr({id: 'pdf-iframe', src:url, width:'100%', height: $(window).height(), border:'none' }).appendTo('div#content');
    $("#loading").text('✔');
}

function indicateNoPDF() {
    $("#loading").text('✘');
}

function MRNumber() {
    return "MR" + $("div.headlineMenu a:contains('Make Link')").attr('href').replace("http://www.ams.org/mathscinet-getitem?mr=", "");
}

function PDFFileName() {
    return MRNumber() + ".pdf";
}

function findPDFURL(metadata, callback) {
    if(metadata.MRNUMBER) {
        TODO
        // check the local filesystem
    }
    if(metadata.URL) {
        if(metadata.URL.startsWith("http://dx.doi.org/10.1006") || metadata.URL.startsWith("http://dx.doi.org/10.1016")) {
            // handle Elsevier separately
            loadAsync(metadata.URL, function(response) {
                      var regex = /pdfurl="([^"]*)"/;
                      callback(regex.exec(response)[1]);
              });
        } else if(metadata.URL.startsWith("http://dx.doi.org/10.1017/S") || metadata.URL.startsWith("http://dx.doi.org/10.1051/S") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S0010437X") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S14611570") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S00255793")) {
            // Cambridge University Press
            loadAsync(metadata.URL, function(response) {
                      var regex = /<a href="([^"]*)"\s*title="View PDF" class="article-pdf">/;
                      callback("http://journals.cambridge.org/action/" + regex.exec(response)[1].trim());
            });
        } else if(metadata.URL.startsWith("http://dx.doi.org/10.1002/")) {
            // Wiley
            loadAsync("http://onlinelibrary.wiley.com/doi/" + metadata.URL.slice(18) + "/pdf", function(response) {
                      var regex = /id="pdfDocument" src="([^"]*)/;
                      callback(regex.exec(response)[1]);
                });
        } else {
            if(metadata.URL.startsWith("http://dx.doi.org/")) {
                loadJSON(
                         metadata.URL.replace("http://dx.doi.org/", "http://evening-headland-2959.herokuapp.com/"),
                         function (data) { if(data.redirect) callback(data.redirect); }
                         );
            };
            if(metadata.URL.startsWith("http://projecteuclid.org/getRecord?id=")) {
                callback(metadata.URL.replace("http://projecteuclid.org/getRecord?id=", "http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle="));
            }
            if(metadata.URL.startsWith("http://www.numdam.org/item?id=")) {
                callback(metadata.URL.replace("http://www.numdam.org/item?id=", "http://archive.numdam.org/article/") + ".pdf");
            };
        }
    }
}

function rewriteArticleLinks() {
    var elements = $("a:contains('Article'), a:contains('Chapter'), a:contains('Thesis'), a:contains('Book')");
    elements.attr('href', function() { return this.href.replace(/http:\/\/[^\/]*\/leavingmsn\?url=/,""); });
    var eventually = function(link) { };
    if(elements.length == 1) {
        eventually = function(link) {
            if(link.href.indexOf("http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle=euclid.dmj/") == 0) {
                showInIFrame(link.href);
            } else if(link.href.indexOf("pdf") !== -1 || link.href.indexOf("displayFulltext") !== -1 /* CUP */) {
                $("a:contains('Article'), a:contains('Chapter'), a:contains('Thesis'), a:contains('Book')").after($('<span/>').attr({id: 'loading'}).text('…'))                
                processPDFLink(link.href);
            }
        }
    }
    elements.each(function() {
          var link = this
          findPDFURL({ URL: this.href }, function(url) {
                     link.href = url;
                     eventually(link);
                     });
        });
}



if (typeof String.prototype.startsWith != 'function') {
    String.prototype.startsWith = function (str){
        return this.slice(0, str.length) == str;
    };
}

main()
