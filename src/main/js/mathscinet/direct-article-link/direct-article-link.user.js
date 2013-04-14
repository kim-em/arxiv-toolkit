function main() {    
  console.log("direct-article-link.user.js starting up on " + location.href + " at " + new Date().getTime());

  verifyEncoding("a perfectly safe string, hopefully");

  insertMenuItem();
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

function verifyEncoding(string) {
  // var zip = new JSZip();
  // zip.file(string, string);
  // window.saveAs(zip.generate({type:"blob"}),string + ".zip");
}

function insertMenuItem() {
  $("ul#menu").prepend($("<li/>").attr({ class: 'first' }).append($("<a/>").text("PDFs").click(function() { switchToPDFViewer(); })));

  function showFiles(filter) {
   findFilesByName(function(name) { return name.indexOf(filter) !== -1; }, continuation);
   function continuation(files) {
    $("#FileList").children().remove();
    files.forEach(function(file) {
      verifyEncoding(file.name);
      var li = $("<li/>");
      li
      .append($("<a/>").text("x").css('color', 'red').click(function() { deleteFile(file.name); li.remove(); }))
      .append($("<a/>").attr({ href: file.toURL(), download: file.name  }).append(downloadIcon()))
      .append($("<a/>").text(file.name).attr({ class: 'pdf', href: file.toURL() }))
      $("#FileList").append(li);
    });
  }
}

function switchToPDFViewer() {
  $("#everything").hide();
  $("#everything").after($("<div/>").load(chrome.extension.getURL('PDFViewer.html'), function() {
    $("#return").click(switchBack);
    $("#filter").keyup(function(event) { showFiles(this.value); });
    $("#delete-all").click(function() {
      $("#FileList a.pdf").each(function() {
        deleteFile($(this).text());
        $(this).parent().remove();
      });
    });
    $("#download-all").click(function() {
      $("#FileList a.pdf").each(function() {
        var name = $(this).text();
        loadBlob(this.href, function(blob) {
          window.saveAs(blob, name);
        });
      });
    });
    $("#download-zip").click(function() {
      console.log("Creating zip file...");
      var zip = new JSZip();
      console.log("JSZip thinks it can handle arraybuffer: ", JSZip.support.arraybuffer);
      console.log("JSZip thinks it can handle blob: ", JSZip.support.blob);
      var count = $("#FileList a.pdf").length;
      $("#FileList a.pdf").each(function() {
        var name = $(this).text();
        console.log("...requesting zip entry for " + name);
        window.resolveLocalFileSystemURL(this.href, function(fileEntry) {
          fileEntry.file(function(file) {
            console.log("...obtained file entry for " + name);
            readAsArrayBuffer(file, function(buffer) {
              console.log("...obtained array buffer for " + name);
              zip.file(name, buffer, { binary: true });
              count--;
              console.log("..." + count + " entries remaining")
              if(count == 0) {
                console.log("...initiating save")
                window.saveAs(zip.generate({type:"blob", compression:"STORE"}), "papers.zip");
              }
            });
          });
        });
      });
    });
    showFiles("");
  }));
}

function switchBack() {
  $("#PDFViewer").remove();
  $("#everything").show();
}

}

// In berserk mode, we try to process all the PDFs on a search page.
// It's actually not that insane, although authentication dialogs from the AMS pop up haphazardly.
var berserk = false;

function processPDF(metadata) {
  loadBlob(metadata.PDF, function(blob) {
    verifyBlob(blob, function(blob) {
      metadata.blob = blob;
      forkCallback([ saveToFileSystem, showInIFrame ])(metadata)
    }, function() { indicateNoPDF(metadata); })
  });
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

function filename(metadata) {
  if(metadata.citation) {
    return metadata.citation + ".pdf";
  } else {
    return metadata.MRNUMBER + ".pdf";
  }
}

function saveBlobToFileSystem(blob, filename) {
  function errorHandler(error) { console.log("An error occurred while saving to the chrome file system: ", error); }

  if(typeof fileSystem !== "undefined") {
    fileSystem.root.getFile(filename, {create: true}, function(fileEntry) {
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
  } else {
    console.log("Warning: chrome file system not available.");
  }  
}

function saveToFileSystem(metadata) {

  if(metadata.PDF.indexOf("filesystem:") === 0) {
    console.log("PDF is already in the chrome file system");
    return;
  } else {
    saveBlobToFileSystem(metadata.blob, filename(metadata));
  }

}

function showInIFrame(metadata) {
  var url
  if(metadata.PDF.indexOf("filesystem:") === 0) {
    url = metadata.PDF;
  } else {
    url = window.URL.createObjectURL(metadata.blob);
  }
  if(!berserk) {
    var iframe = $('<iframe/>').attr({id: 'pdf-iframe', src:url, width:'100%', height: $(window).height(), border:'none' }).appendTo('div#content');
  }
  $("#loading" + metadata.MRNUMBER).text('').append($("<a/>").attr({ href: url, download: filename(metadata) }).append(downloadIcon));
}

function downloadIcon() {
  return $("<img/>").attr({ width: '25px', style: 'vertical-align:-30%;', src: chrome.extension.getURL('download.svg') });
}

function indicateNoPDF(metadata) {
  $("#loading" + metadata.MRNUMBER).text('✘');
}

// Given some metadata, tries to find a URL for the PDF, and if successful calls the callback function with the metadata know containing a "PDF" field.
function findPDF(metadata, callback, allowScraping) {
  console.log("Attempting to find PDF for " + JSON.stringify({ URL: metadata.URL, MRNUMBER: metadata.MRNUMBER, citation: metadata.citation }));
  function doCallback(url) {
    metadata.PDF = url;
    callback(metadata);
  }

  // First, check the chrome file system, in case we've collected it previously.
  if(metadata.MRNUMBER) {
    findFilesByName(function(name) { return name.indexOf(metadata.MRNUMBER) !== -1; }, continuation);
  } else {
    continuation([]);
  }

  function continuation(files) {
    if(files.length == 1) {
      console.log("Found PDF in local file system.");
      doCallback(files[0].toURL());
      return;
    }
    if(files.length !== 0) {
      console.log("Strange, I found multiple PDFs for " + JSON.stringify({ MRNUMBER: metadata.MRNUMBER }));
    }
    if(metadata.URL) {
      if(metadata.URL.startsWith("http://dx.doi.org/10.1006") || metadata.URL.startsWith("http://dx.doi.org/10.1016")) {
              // handle Elsevier separately
              if(allowScraping) {
              loadAsync(metadata.URL, function(response) {
                var regex = /pdfurl="([^"]*)"/;
                doCallback(regex.exec(response)[1]);
                return;
              });
            }
            } else if(metadata.URL.startsWith("http://dx.doi.org/10.1017/S") || metadata.URL.startsWith("http://dx.doi.org/10.1017/is") || metadata.URL.startsWith("http://dx.doi.org/10.1051/S") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S0010437X") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S14611570") || metadata.URL.startsWith("http://dx.doi.org/10.1112/S00255793")) {
              // Cambridge University Press
              if(allowScraping) {
              loadAsync(metadata.URL, function(response) {
                var regex = /<a href="([^"]*)"\s*title="View PDF" class="article-pdf">/;
                doCallback("http://journals.cambridge.org/action/" + regex.exec(response)[1].trim());
                return; 
              });
            }
            } else if(metadata.URL.startsWith("http://dx.doi.org/10.1002/")) {
              // Wiley
              if(allowScraping) {
              loadAsync("http://onlinelibrary.wiley.com/doi/" + metadata.URL.slice(18) + "/pdf", function(response) {
                var regex = /id="pdfDocument" src="([^"]*)/;
                doCallback(regex.exec(response)[1]);
                return;
              });
            }
            } else if(metadata.URL.startsWith("http://dx.doi.org/")) {
              loadJSON(
               metadata.URL.replace("http://dx.doi.org/", "http://evening-headland-2959.herokuapp.com/"),
               function (data) { if(data.redirect) doCallback(data.redirect); }
               );
              return;
            } else if(metadata.URL.startsWith("http://projecteuclid.org/getRecord?id=")) {
              doCallback(metadata.URL.replace("http://projecteuclid.org/getRecord?id=", "http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle="));
              return;
            } else if(metadata.URL.startsWith("http://www.numdam.org/item?id=")) {
              doCallback(metadata.URL.replace("http://www.numdam.org/item?id=", "http://archive.numdam.org/article/") + ".pdf");
              return;
            }
          }
        }
      }

      function rewriteArticleLinks() {
        var metadataDivs = $("div.headline");
        console.log("Found " + metadataDivs.length + " metadata divs.");

        // First, strip out all the "leavingmsn" prefixes
        metadataDivs.find("a").attr('href', function() { return this.href.replace(/http:\/\/[^\/]*\/leavingmsn\?url=/,""); });

        function extractMetadata(div) {
          var link = $(div).find("a:contains('Article'), a:contains('Chapter'), a:contains('Thesis'), a:contains('Book')")
          var URL = link.attr('href');
          var MRNUMBER = $(div).find("strong").first().text();
          var h = $(div).clone();
          h.find(".item_status").remove();
          h.find("span.MathTeX").remove();
          if(h.find("div.checkbox").length !== 0) {
            h = h.find("div.headlineText").first();
            // we're looking at a search results page
            // chuck stuff away
            h.find("a[href*=mscdoc]").nextAll().remove();
            h.find("a[href*=mscdoc]").remove();
            h.find(".sfx").nextAll().remove();
            h.find(".sfx").remove();
            // insert dashes
            h.find("a.mrnum").after(" - ");
            h.find("span.title").before(" - ");
            h.find("span.title").after(" - ");
          } else {
            // we're on an article page
            // chuck stuff away
            h.find(".sfx").remove();
            h.find("strong").eq(1).remove();
            h.find("a[href*=institution]").remove();
            h.find("br").eq(3).nextAll().remove();
            h.find("br").eq(3).remove();
            // insert dashes
            h.find("br").replaceWith(" - ");
          }
          // cleanup
          h.contents().filter(function() { return this.nodeType === 3 && this.textContent === "; "; }).replaceWith(" and ");
          var citation = h.text().replace(/\(Reviewer: .*\)/, '').replace(/\s+/g, ' ').trim();
          verifyEncoding(citation);
          return { URL: URL, MRNUMBER: MRNUMBER, div: div, link: link, citation: citation }
        }

        var eventually = function(metadata) { };
        if(metadataDivs.length == 1 || berserk) {
          eventually = function(metadata) {
            var href = metadata.link.attr('href');
            if(href.indexOf("http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle=euclid.dmj/") == 0) {
              showInIFrame(href);
            } else if(href.indexOf("pdf") !== -1 || href.indexOf("displayFulltext") !== -1 /* CUP */) {
              metadata.link.after($('<span/>').attr({id: 'loading' + metadata.MRNUMBER}).text('…'))                
              processPDF(metadata);
            }
          }
        }
        metadataDivs.each(function() {
          var metadata = extractMetadata(this);
          findPDF(metadata, function(metadata) {
            if(metadata.PDF) {
             metadata.link.attr('href', metadata.PDF);
             eventually(metadata);
           }
         }, metadataDivs.length == 1 || berserk);
        });
      }



      if (typeof String.prototype.startsWith != 'function') {
        String.prototype.startsWith = function (str){
          return this.slice(0, str.length) == str;
        };
      }

      main()
