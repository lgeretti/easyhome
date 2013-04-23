(function($) {

	function getCount() {
	   $.ajax({ 
	       type: "GET",
	       dataType: "json",
	       url: "rest/counter",
	       success: function(data){        
	          $(".pushedText").html(data.count);
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
