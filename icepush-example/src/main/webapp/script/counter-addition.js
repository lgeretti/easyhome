(function($) {

	function getCount() {
	   $.ajax({ 
	       type: "GET",
	       dataType: "text",
	       url: "rest/count",
	       success: function(data){        
	          $(".pushedText").html(data);
	       }
	   });
	}
	
	$(document).ready(
			
		function() {
  			getCount();
			$.push.listenToGroup("counter",getCount);
		}
	);

})(jQuery);
