(function($) {

		$(document).ready(
    		function() {
    			
    			var u = 0;
    			
    			$.push.listenToGroup("counter", 
    					function() {
    			      		$(".pushedText").append(u);
    			      		u++;
    					}
    			);     			
    		}
    	);

})(jQuery);
