// Constants - Status Updates
var STRATEGIZING_STATUS = "Your opponent is strategizing.";
var WAITING_STATUS = "Waiting for an opponent.";
var YOUR_TURN_STATUS = "It's your turn!";
var YOU_WIN_STATUS = "You win!";
var YOU_LOSE_STATUS = "You lose!";
var TIED_STATUS = "The game is tied.";
var WEBSOCKET_CLOSED_STATUS = "The WebSocket Connection Has Been Closed.";

// Constants - Game
var PLAYER_O = "O";
var PLAYER_X = "X";

// Constants - Incoming message types
var MESSAGE_HANDSHAKE = "handshake";
var MESSAGE_OPPONENT_UPDATE = "response";
var MESSAGE_TURN_INDICATOR = "turn";
var MESSAGE_GAME_OVER = "GAME_OVER";

// Constants - Message turn indicator types
var MESSAGE_TURN_INDICATOR_YOUR_TURN = "YOUR_TURN";
var MESSAGE_TURN_INDICATOR_WAITING = "WAITING";

// Constants - Game over message types
var MESSAGE_GAME_OVER_YOU_WIN = "YOU_WIN";
var MESSAGE_GAME_OVER_TIED = "TIED";

// Constants - WebSocket URL
var WEBSOCKET_URL = "ws://localhost:9000/websocket";


// Variables
var player;
var opponent;
var gameId;
var yourTurn = false;

// WebSocket connection
var ws;

$(document).ready(function() {

	/* Bind to the click of all divs (tic tac toe cells) on the page
	   We would want to qualify this if we styled the game fancier! */
	$("div").click(function () {
		// Only process clicks if it's your turn.
		if (yourTurn == true) { 
	      // Stop processing clicks and invoke sendMessage(). 
		  yourTurn = false;
    	  sendTurnMessage(this.id);
    	  // Add the X or O to the game board and update status.
	      $("#" + this.id).addClass(player);
	      $("#" + this.id).html(player);	    	  
	      $('#status').text(STRATEGIZING_STATUS);    	 					      
    	}
    });	

	// On the intial page load we perform the handshake with the server.
    ws = new WebSocket(WEBSOCKET_URL);
    
    ws.onopen = function(event) { 
    	$('#status').text(WAITING_STATUS);
    }
    
    // Process turn message ("push") from the server.
	ws.onmessage = function(event) {
 		var message = jQuery.parseJSON(event.data);

 		console.debug(message);
 		
 		// Process the handshake response when the page is opened
 		if (message.messageType === MESSAGE_HANDSHAKE) {
   	 		player = message.playerLetter;
   	 		console.debug("your player letter is " + player);

   	 	 	if (player === PLAYER_X) {
   	 	 		opponent = PLAYER_O;
   	 	 		console.debug("your opponent is O");
   	 	 	} else {
   	 	 		opponent = PLAYER_X;
   	 	 		console.debug("your opponent is X");
   	 	 	}
 		}
 		
 		// Process your opponent's turn data.
 		if (message.messageType === MESSAGE_OPPONENT_UPDATE) {
 			// Show their turn info on the game board.
 			$("#grid_" + message.gridId).addClass(opponent);
 			$("#grid_" + message.gridId).html(opponent);

 			// Switch to your turn.
 			if (message.status == MESSAGE_GAME_OVER_YOU_WIN) {
 				$('#status').text(message.opponent + " is the winner!");
 			} else if (message.status == MESSAGE_GAME_OVER_TIED) {
 				$('#status').text(TIED_STATUS);
 			} else {
 				yourTurn = true;
    			$('#status').text(YOUR_TURN_STATUS);    	   	 			
    		}
 		}   	 	
 		
 		/* The initial turn indicator from the server. Determines who starts
 		   the game first. Both players wait until the server gives the OK
 		   to start a game. */
 		if (message.messageType === MESSAGE_TURN_INDICATOR) {
 			if (message.turnIndicator === MESSAGE_TURN_INDICATOR_YOUR_TURN) {
 				yourTurn = true;
	    		$('#status').text(YOUR_TURN_STATUS);    	 			
    		} else if (message.turnIndicator === MESSAGE_TURN_INDICATOR_WAITING) {
				$('#status').text(STRATEGIZING_STATUS);    	 					    	
    		}
 		}

        /* The server has determined you are the winner and sent you this message. */
        if (message.messageType === MESSAGE_GAME_OVER) {
            if (message.status === MESSAGE_GAME_OVER_TIED) {
                $('#status').text(TIED_STATUS);
            } else if (message.status === MESSAGE_GAME_OVER_YOU_WIN) {
                $('#status').text(YOU_WIN_STATUS);
            }
            else {
                $('#status').text(YOU_LOSE_STATUS);
            }
        }
 	} 
 	
 	ws.onclose = function(event) { 
 		$('#status').text(WEBSOCKET_CLOSED_STATUS); 
 	} 

});

// Send your turn information to the server.
function sendTurnMessage(id) {
	var message = {messageType:"TURN", gridId:id};
	var encoded = $.toJSON(message);
	ws.send(encoded);
};
