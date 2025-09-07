//package com.Chanalyst.ChanalystV1.Controller.RestControllers;
//
//import com.Chanalyst.ChanalystV1.Entity.Player;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/leaderboard")
//public class LeaderboardController {
//
//    @GetMapping
//    public ResponseEntity<List<Player>> getLeaderboard(@RequestParam String roomId) {
//        // Return sorted leaderboard
//    }
//
//    @GetMapping("/final")
//    public ResponseEntity<List<Player>> getFinalLeaderboard(@RequestParam String roomId) {
//        // Final leaderboard at game end
//    }
//}