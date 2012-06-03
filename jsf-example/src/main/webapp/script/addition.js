(function($) {

jQuery(document).ready(
    		function() {
    			
                $(".hiddenBridge").change( function() {

                    $(".salutation").append("Hello!");
                
                });

    		    $(".anyClass").append("<ul><li>1st</li><li>2nd</li><li>3rd</li><li>4th</li></ul>")
    			.jCarouselLite({
                    btnNext: ".next",
                    btnPrev: ".prev"
                });
 			
});

})(jQuery);
