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
