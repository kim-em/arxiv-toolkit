var ids = ["#berserk","#inline","#filename","#download","#download-zip", "#store","#store-synchronized","#dropbox","#drive","#mega","#mega-account"];

function save_options() {
  console.log("saving options");
  var items = {};

  for (var i = 0; i < ids.length; i++) {
    var id = ids[i];
    var select = $(id);
    var value
    if(select.is(":checkbox")) {
      value = select.is(':checked');
    } else {
      value = select.val();
    }
    items[id] = value;
  }
  console.log(JSON.stringify(items));
  chrome.storage.sync.set(items);
}

// Restores select box state to saved value from localStorage.
function restore_options() {
  console.log("restoring options");
  chrome.storage.sync.get(ids, function(items) {
    console.log(JSON.stringify(items));
    for (var i = 0; i < ids.length; i++) {
      var id = ids[i];
      if(typeof items[id] !== "undefined") {
        var select = $(id);
        if(select.is(":checkbox")) {
          select.prop('checked', items[id]);
        } else {
          select.val(items[id])
        }
      }
    }
  });
}

document.addEventListener('DOMContentLoaded', restore_options);
for (var i = 0; i < ids.length; i++) {
  document.querySelector(ids[i]).addEventListener('click', save_options);
}
