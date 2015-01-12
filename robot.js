try {

	function getOffset( el ) {
	    var _x = 0;
	    var _y = 0;
	    while( el && !isNaN( el.offsetLeft ) && !isNaN( el.offsetTop ) ) {
	        _x += el.offsetLeft - el.scrollLeft;
	        _y += el.offsetTop - el.scrollTop;
	        el = el.offsetParent;
	    }
	    return { top: _y, left: _x };
	}
	function obValsl(ob){var r=[],mx=ob.length;for(var z=0;z<mx;z++){r[z]=ob[z];}return r;}
	function tags(tagName){ return obValsl(document.getElementsByTagName(tagName));};
	
	form = document.getElementById("coralExp");

	if (form) {

	        document.body.style.zoom = 0.4;


			var formsCollection = tags("input").concat( tags("select").concat( tags("textarea") ) );
//			var formsCollection = form.getElementsByTagName("select");
//			formsCollection = formsCollection.concat(form.getElementsByTagName("select"));
				
			for(var i=0;i<formsCollection.length;i++)
			{
			   	// alert(formsCollection[i].name);
			   	
			   	var ref = formsCollection[i]; 
			   	
				if ( ref.name == "number" ) {
				    ref.value = 100;
                }

	         	if ( ref.name == "val" ) {
	                ref.value = Math.round(Math.random()*60+1000)/100;
	            }

				if ( ref.name == "continue" ) {
					setTimeout( function f() { form.submit() } , Math.round(1000*Math.random())+1000 );
				}

				if ( ref.name == "next" ) {
					setTimeout( 'window.location = "exp://host/?skiperror=true";', Math.round(200*Math.random())+1000);
				}

			}
            // code to create screenshots
	    var scrnid = form['_scrnid'];
	    if ( scrnid != null) {
	    	// window.location = "scrn:" + scrnid.value + ".png";
	    }
	    
	} else {
//		setTimeout( 'window.location = "exp://host/?skiperror=true";', Math.round(1000*Math.random())+1000);
	}
} catch(err) {
	
	alert(err.message + "\n " + err.stack);
}

