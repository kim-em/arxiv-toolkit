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

function blob2Text(blob, callback) {
    var f = new FileReader();
    f.onload = function(e) {
        callback(e.target.result)
    }
    f.readAsText(blob);
}

function dataURItoBlob(dataURI) {
    // convert base64 to raw binary data held in a string
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
