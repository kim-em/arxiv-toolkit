/* TODO Somehow make sure this only runs when appropriate,
 * perhaps by coordinating with the background page? */
console.log("We're at: " + location.href);
var PDF = $("div.download-section-div a:contains('PDF File')").first().attr('href');
console.log("Found PDF link: " + PDF);
var regex = /http:\/\/projecteuclid\.org\/(euclid\.[a-z]*\/[0-9]*)/;
var handle = regex.exec($("div#identifier").text())[1];
console.log("Found handle: " + handle);


loadBlob(PDF, function(blob) {
	readAsDataURL(blob, function(uri) {
      console.log("Sending a 'saveToDropbox' request to the background page.")
      chrome.runtime.sendMessage({cmd: "saveToDropbox", metadata: {handle: handle, uri: uri} });
	location.href = window.URL.createObjectURL(blob);
	});
});

