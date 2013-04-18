// These keys also appear in Settings.js
var settingsKeys = ["#berserk","#inline","#filename", "#download","#download-zip", "#store","#store-synchronized","#dropbox","#drive","#mega","#mega-account"];
var settings = {};
var papersSavedInDropbox = {}; // a map from MRNUMBERs to filenames // Filenames are being encoding-mangled, don't trust them...

var onSearchPage = false;

// In berserk mode, we try to process all the PDFs on a search page.
// It's actually not that insane, although authentication dialogs from the AMS pop up haphazardly.
// TODO Move all the loadBlob requests to the background page, so those get ignored.

function main() {    
  console.log("direct-article-link.user.js starting up on " + location.href + " at " + new Date().getTime());

  insertMenuItem();

  chrome.storage.sync.get(settingsKeys, function(items) {
    if(Object.keys(items).length == 0) {
      alert("Welcome to the 'MathSciNet direct links' extension." + "\n" +
        "Click the 'library' icon in the address bar to adjust your settings." + "\n" +
        "Click 'PDFs' in the MathSciNet toolbar to view and manage cached PDFs.");
      items = { "#inline": true, "#store": true, "#dropbox": true };
      chrome.storage.sync.set(items);
    }
    settings = items;
    if(settings["#dropbox"]) {
      chrome.runtime.sendMessage({cmd: "listPapersSavedInDropbox"}, function(response) {
        // Warning: somehow chrome is mangled the character encoding here, so don't trust the filenames in papersSavedInDropbox
        papersSavedInDropbox = response.papersSavedInDropbox;
        rewriteArticleLinks();
      })
    } else {
      rewriteArticleLinks();
    }
  });

}

function insertMenuItem() {
  $("ul#menu").prepend($("<li/>").attr({ class: 'first' }).append($("<a/>").text("PDFs").click(function() { switchToPDFViewer(); })));

  function showFiles(filter) {
   findFilesByName(function(name) { return name.indexOf(filter) !== -1; }, continuation);
   function continuation(files) {
    $("#FileList").children().remove();
    files.forEach(function(file) {
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
      console.log("Creating zip file...");
      var zip = new JSZip();
      var count = $("#FileList a.pdf").length;
      $("#zip-progress").show();
      $("#zip-counter").text(count);
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
              $("#zip-counter").text(count);
              console.log("..." + count + " entries remaining")
              if(count == 0) {
                console.log("...initiating save")
                window.saveAs(zip.generate({type:"blob", compression:"STORE"}), "papers.zip");
                $("#zip-progress").hide();
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


function processPDF(metadata) {
  console.log("Beginning processPDF on " + metadata.MRNUMBER);
  if(settings["#inline"] || settings["#store"] || settings["#download"] || settings["#dropbox"]) {
    metadata.link.after($('<span/>').attr({id: 'loading' + metadata.MRNUMBER}).text('…'))                
    loadBlob(metadata.PDF, function(blob) {
      verifyBlob(blob, function(blob) {
        metadata.blob = blob;
        forkCallback([ saveToFileSystem, showInIFrame, generateDownload, saveToDropbox ])(metadata)
      }, function() { indicateNoPDF(metadata); })
    });
  }
}

function forkCallback(callbacks) {
  return function(response) {
    callbacks.forEach(function(callback) { callback(response); } );
  }
}

function verifyBlob(blob, success, failure) {
  readAsText(blob.slice(0, 10), function(text) {
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
  var template = settings["#filename"];
  if(typeof template === "undefined" || template.indexOf("$MRNUMBER") === -1) {
    template = "$MRNUMBER - $AUTHORS - $TITLE - $JOURNALREF.pdf"
  }
  template = template.replace(/\$MRNUMBER/gi, metadata.MRNUMBER);
  template = template.replace(/\$AUTHORS/gi, metadata.authors);
  template = template.replace(/\$TITLE/gi, metadata.title);
  template = template.replace(/\$JOURNALREF/gi, metadata.journalRef);
  return template;
}

function saveToDropbox(metadata) {
  if(settings["#dropbox"]) {
    if(papersSavedInDropbox[metadata.MRNUMBER]) {
      console.log("... already saved in dropbox.");
    } else {
      console.log("Packing metadata to send to the background page.")
      packMetadata(metadata, function(packedMetadata) {
        console.log("Sending a 'saveToDropbox' request to the background page.")
        chrome.runtime.sendMessage({cmd: "saveToDropbox", metadata: packedMetadata });
      });
    }
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
  if(settings["#store"]) {
    if(metadata.PDF.indexOf("filesystem:") === 0) {
      console.log("PDF is already in the chrome file system");
      return;
    } else {
      saveBlobToFileSystem(metadata.blob, metadata.filename);
    }
  }
}

function generateDownload(metadata) {
  if(settings["#download"]) {
    if(settings["#download-zip"]) {
      var zip = new JSZip();
      readAsArrayBuffer(metadata.blob, function(buffer) {
        zip.file(metadata.filename, buffer, { binary: true });
        window.saveAs(zip.generate({type:"blob", compression:"STORE"}), metadata.filename + ".zip");
      });
    } else {
      window.saveAs(metadata.blob, metadata.filename);
    }
  }
}

function showInIFrame(metadata) {
  if(settings["#inline"]) {
    var url
    if(metadata.PDF.indexOf("filesystem:") === 0 || !(metadata.blob)) {
      url = metadata.PDF;
    } else {
      url = window.URL.createObjectURL(metadata.blob);
    }
    if(!onSearchPage) {
      $('<iframe/>').attr({id: 'pdf-iframe', src:url, width:'100%', height: $(window).height(), border:'none' }).appendTo('div#content');
    }
    if(onSearchPage && metadata.PDF.indexOf("http://projecteuclid.org/") === 0 && settings["#berserk"]) {
      $('<iframe/>').attr({id: 'pdf-iframe-' + metadata.handle, src:url, width:'100%', height: 0, frameborder: 0, style:"border:none; display:none;" }).appendTo('div#content');      
    }
  }
  $("#loading" + metadata.MRNUMBER).text('').append($("<a/>").attr({ href: url, download: metadata.filename }).append(downloadIcon));
}

function downloadIcon() {
  return $("<img/>").attr({ width: '25px', style: 'vertical-align:-30%;', src: chrome.extension.getURL('download.svg') });
}

function indicateNoPDF(metadata) {
  $("#loading" + metadata.MRNUMBER).text('✘');
}

// Given some metadata, tries to find a URL for the PDF, and if successful calls the callback function with the metadata now containing a "PDF" field.
function findPDF(metadata, callback, allowScraping) {
  console.log("Attempting to find PDF for " + JSON.stringify({ URL: metadata.URL, MRNUMBER: metadata.MRNUMBER, citation: metadata.citation }));
  function doCallback(url) {
    metadata.PDF = url;
    callback(metadata);
  }

  // First, check the chrome file system and dropbox, in case we've collected it previously.
  if(metadata.MRNUMBER) {
    findFilesByName(function(name) { return name.indexOf(metadata.MRNUMBER) !== -1; }, function(files) {
      if(files.length > 0) {
        continuation(files);
      } else {
        if(papersSavedInDropbox[metadata.MRNUMBER]) {
          /* Here we get back a data URI from the background page. */
          /* An alternative strategy might have been to obtain a Dropbox download link from the background page. */
          var cmd = {
            cmd: "loadFromDropbox", 
            metadata: { 
              MRNUMBER: metadata.MRNUMBER, 
              filename: metadata.filename 
            } 
          };
          console.log("Sending request: " + JSON.stringify(cmd));
          chrome.runtime.sendMessage(cmd, function(responseMetadata) {
            doCallback(responseMetadata.uri);
          });
        } else {
          continuation([]);
        }
      }
    });
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
      if( // handle Elsevier separately
        metadata.URL.startsWith("http://dx.doi.org/10.1006") || 
        metadata.URL.startsWith("http://dx.doi.org/10.1016")) {
        if(allowScraping) {
          loadAsync(metadata.URL, function(response) {
            var regex = /pdfurl="([^"]*)"/;
            doCallback(regex.exec(response)[1]);
            return;
          });
        }
      } else if( // Cambridge University Press
        metadata.URL.startsWith("http://dx.doi.org/10.1017/S") ||
        metadata.URL.startsWith("http://dx.doi.org/10.1017/is") || 
        metadata.URL.startsWith("http://dx.doi.org/10.1051/S") || 
        metadata.URL.startsWith("http://dx.doi.org/10.1112/S0010437X") || 
        metadata.URL.startsWith("http://dx.doi.org/10.1112/S14611570") || 
        metadata.URL.startsWith("http://dx.doi.org/10.1112/S00255793")) {
        if(allowScraping) {
          loadAsync(metadata.URL, function(response) {
            var regex = /<a href="([^"]*)"\s*title="View PDF" class="article-pdf">/;
                  /*
                  http://journals.cambridge.org/action/displayFulltext?type=1&fid=8143111&jid=EJM&volumeId=22&issueId=02&aid=8143109&bodyId=&membershipNumber=&societyETOCSession=
                  http://journals.cambridge.org/action/displayFulltext?type=1&fid=8143111&jid=EJM&volumeId=22&issueId=02&aid=8143109&newWindow=Y
                  */
                  doCallback("http://journals.cambridge.org/action/" + regex.exec(response)[1].trim() + "&newWindow=Y");
                  return; 
                });
        }
      } else if(metadata.URL.startsWith("http://dx.doi.org/10.1002/")) { // Wiley
        if(allowScraping) {
          loadAsync("http://onlinelibrary.wiley.com/doi/" + metadata.URL.slice(18) + "/pdf", function(response) {
            var regex = /id="pdfDocument" src="([^"]*)/;
            doCallback(regex.exec(response)[1]);
            return;
          });
        }
      } else if(metadata.URL.startsWith("http://dx.doi.org/10.1145/")) { // ACM
        if(allowScraping) {
          loadAsync("http://dl.acm.org/citation.cfm?doid=" + metadata.URL.slice(26), function(response) {
            console.log(response);
            var regex = /title="FullText Pdf" href="(ft_gateway\.cfm\?id=[0-9]*&type=pdf&CFID=[0-9]*&CFTOKEN=[0-9]*)"/;
            doCallback("http://dl.acm.org/" + regex.exec(response)[1]);
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
      } else if(metadata.URL.startsWith("http://aif.cedram.org/item?id=")) {
        doCallback(metadata.URL.replace("http://aif.cedram.org/item?id=", "http://aif.cedram.org/cedram-bin/article/") + ".pdf")
      }
    }
  }
}

function rewriteArticleLinks() {
  var metadataDivs = $("div.headline");
  console.log("Found " + metadataDivs.length + " metadata divs.");

        // First, strip out all the "leavingmsn" prefixes
        metadataDivs.find("a").attr('href', function() { return this.href.replace(/http:\/\/[^\/]*\/leavingmsn\?url=/,""); });

        onSearchPage = metadataDivs.length > 1;

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
            h.find("a.mrnum").after(" %%%% ");
            h.find("span.title").before(" %%%% ");
            h.find("span.title").after(" %%%% ");
          } else {
            // we're on an article page
            // chuck stuff away
            h.find(".sfx").remove();
            h.find("strong").eq(1).remove();
            h.find("a[href*=institution]").remove();
            h.find("br").eq(3).nextAll().remove();
            h.find("br").eq(3).remove();
            // insert dashes
            h.find("br").replaceWith(" %%%% ");
          }
          // cleanup
          h.contents().filter(function() { return this.nodeType === 3 && this.textContent === "; "; }).replaceWith(" and ");
          var citation = h.text().replace(/\(Reviewer: .*\)/, '').replace(/\s+/g, ' ').trim();
          var cs = citation.split("%%%%");
          var authors = cs[1].trim();
          var title = cs[2].trim().replace(/\(English summary\)/, '');
          var journalRef = cs[3].trim();
          citation = MRNUMBER + " - " + authors + " - " + title + " - " + journalRef;
          var m = { URL: URL, MRNUMBER: MRNUMBER, div: div, link: link, citation: citation, authors: authors, title: title, journalRef: journalRef };
          m.filename = filename(m);
          return m;
        }

        var eventually = function(metadata) { };
        if(!onSearchPage || settings["#berserk"]) {
          eventually = function(metadata) {
            var href = metadata.link.attr('href');
            if(href.indexOf("http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle=euclid.") == 0) {
              if(settings["#inline"]) {
                var cmd = {
                  cmd: "mentionEuclidHandle",
                  handle: href.replace("http://projecteuclid.org/DPubS/Repository/1.0/Disseminate?view=body&id=pdf_1&handle=", ""),
                  metadata: {
                    MRNUMBER: metadata.MRNUMBER,
                    filename: metadata.filename
                  }
                }
                console.log("Sending a request: " + JSON.stringify(cmd));
                chrome.runtime.sendMessage(cmd, function(response) {
                  console.log("Incredible, a PDF arrived back via the iframe and background page.");
                  metadata.PDF = response.uri;
                  processPDF(metadata);
                });
                showInIFrame({ PDF: href, handle: cmd.handle });
              }
            } else if(href.indexOf("pdf") !== -1 || href.indexOf("displayFulltext") !== -1 /* CUP */) {
              processPDF(metadata);
            }
          }
        }
        metadataDivs.each(function() {
          var metadata = extractMetadata(this);
          findPDF(metadata, function(metadata) {
            if(metadata.PDF) {
              console.log("Found PDF link: " + metadata.PDF);
              metadata.link.attr('href', metadata.PDF);
              eventually(metadata);
            }
          }, metadataDivs.length == 1 || settings["#berserk"]);
        });
      }



      if (typeof String.prototype.startsWith != 'function') {
        String.prototype.startsWith = function (str){
          return this.slice(0, str.length) == str;
        };
      }

      main()
