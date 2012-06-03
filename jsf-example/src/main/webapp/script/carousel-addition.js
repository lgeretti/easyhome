(function($) {

	$(document).ready(
	    		function() {
	    			
	    		    $(".myJCarouselLite").append("<ul><li>1st</li><li>2nd</li><li>3rd</li><li>4th</li></ul>")
	    			.jCarouselLite({
	                    btnNext: ".next",
	                    btnPrev: ".prev"
	                });
	    		    
	    		    var u = 0;
	    		    
	    			$.push.listenToGroup("carousel", 
	    					function() {

	        			      	$(".pushedText").append(u);
	        			      	u++;
	    					}
	    			);  
	 			}
	);

})(jQuery);
