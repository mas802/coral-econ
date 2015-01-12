/*
 * Example robot.js file for a simple robot to run through the PGexample
 *
 * Markus Schaffner, 09/07/2013
 */

try {

  // get the current form element
  var form = document.getElementById("examplePG");

  if ( form ) {

    /* 
     * the following code allows to iterate over all 
     * form elements present in a form, shortened 
     * as not relevant here.
     */
    function obValsl(ob){var r=[],mx=ob.length;for(var z=0;z<mx;z++){r[z]=ob[z];}return r;}
    function tags(tagName){return obValsl(document.getElementsByTagName(tagName));};
    var formsCollection = tags("input").concat(tags("select").concat(tags("textarea")));
      
    for( var i=0; i<formsCollection.length; i++) {
      var currentinput = formsCollection[i]; 
     
      // if a input called number is present, set it to 12345
      if ( currentinput.name == "number" ) {
        currentinput.value = '12345';
      }

      // if a input contribution is present, set it to 5
      if ( currentinput.name == "contribution" ) {
        currentinput.value = '5';
      }
      
      // if an input called continue is present, submit the form in 1-1.2 seconds
      if ( currentinput.name == "continue" ) {
        setTimeout( function f() { form.submit() } , Math.round(200*Math.random())+1000 );
      }
    }
  }
} catch(err) {
  alert(err.message + "\n " + err.stack);
}
