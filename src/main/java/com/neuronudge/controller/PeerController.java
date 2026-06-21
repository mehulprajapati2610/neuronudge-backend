package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.model.Notification.NotificationType;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/peer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PeerController {

    private final PeerRoomRepository        peerRoomRepository;
    private final PeerMessageRepository     peerMessageRepository;
    private final PeerJoinRequestRepository joinRequestRepository;
    private final UserRepository            userRepository;
    private final NotificationService       notificationService;
    private final JwtUtil                   jwtUtil;

    // ─── CREATE a room ────────────────────────────────────────────────────────
    @PostMapping("/rooms/create")
    public ResponseEntity<?> createRoom(@RequestBody CreateRoomRequest req,
                                        @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));

        String icon = iconForCategory(req.category());

        PeerRoom room = PeerRoom.builder()
                .ownerId(userId)
                .ownerName(user.getName())
                .topic(req.name())
                .description(req.description())
                .icon(icon)
                .category(req.category())
                .visibility(req.visibility())
                .maxMembers(req.maxMembers())
                .memberIds(new ArrayList<>(List.of(userId)))  // owner auto-joined
                .createdAt(LocalDateTime.now())
                .active(true)
                .build();

        peerRoomRepository.save(room);
        return ResponseEntity.ok(Map.of("message", "Room created", "roomId", room.getId()));
    }

    // ─── GET all rooms (public + private) ────────────────────────────────────
    @GetMapping("/rooms")
    public ResponseEntity<?> getAllRooms(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<PeerRoom> rooms = peerRoomRepository.findByActiveTrue();
        return ResponseEntity.ok(rooms.stream().map(r -> buildRoomMap(r, userId)).collect(Collectors.toList()));
    }

    // ─── GET my rooms (joined or owned) ──────────────────────────────────────
    @GetMapping("/rooms/mine")
    public ResponseEntity<?> getMyRooms(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<PeerRoom> rooms = peerRoomRepository.findByMemberIdsContainingAndActiveTrue(userId);
        return ResponseEntity.ok(rooms.stream().map(r -> buildRoomMap(r, userId)).collect(Collectors.toList()));
    }

    // ─── GET single room info ─────────────────────────────────────────────────
    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId,
                                         @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(buildRoomMap(room, userId));
    }

    // ─── DIRECT JOIN a public room ────────────────────────────────────────────
    @PostMapping("/rooms/{roomId}/join")
    public ResponseEntity<?> joinPublicRoom(@PathVariable String roomId,
                                            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));

        if (!"PUBLIC".equals(room.getVisibility())) {
            return ResponseEntity.status(403).body(Map.of("message", "This is a private room. Send a join request instead."));
        }
        if (room.getMemberIds().contains(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already a member"));
        }
        if (room.getMaxMembers() > 0 && room.getMemberIds().size() >= room.getMaxMembers()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Room is full"));
        }

        room.getMemberIds().add(userId);
        peerRoomRepository.save(room);
        return ResponseEntity.ok(Map.of("message", "Joined successfully"));
    }

    // ─── REQUEST to join a private room ──────────────────────────────────────
    @PostMapping("/rooms/{roomId}/request")
    public ResponseEntity<?> requestJoin(@PathVariable String roomId,
                                         @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (user == null) return ResponseEntity.badRequest().body(Map.of("message", "User not found"));

        if (!"PRIVATE".equals(room.getVisibility())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Use the join endpoint for public rooms"));
        }
        if (room.getMemberIds().contains(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Already a member"));
        }
        if (room.getMaxMembers() > 0 && room.getMemberIds().size() >= room.getMaxMembers()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Room is full"));
        }

        Optional<PeerJoinRequest> existing = joinRequestRepository.findByRoomIdAndRequesterId(roomId, userId);
        if (existing.isPresent() && "PENDING".equals(existing.get().getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Request already sent"));
        }

        PeerJoinRequest joinReq = PeerJoinRequest.builder()
                .roomId(roomId)
                .requesterId(userId)
                .requesterName(user.getName())
                .status("PENDING")
                .requestedAt(LocalDateTime.now())
                .build();
        joinRequestRepository.save(joinReq);

        notificationService.create(
                room.getOwnerId(),
                NotificationType.GENERAL,
                "Join Request — " + room.getTopic(),
                user.getName() + " wants to join your private room \"" + room.getTopic() + "\".",
                "peer.html"
        );

        return ResponseEntity.ok(Map.of("message", "Join request sent to the room owner"));
    }

    // ─── APPROVE a join request ───────────────────────────────────────────────
    @PostMapping("/rooms/{roomId}/approve/{requesterId}")
    public ResponseEntity<?> approveRequest(@PathVariable String roomId,
                                            @PathVariable String requesterId,
                                            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Only the owner can approve requests"));
        }

        PeerJoinRequest req = joinRequestRepository.findByRoomIdAndRequesterId(roomId, requesterId).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();

        req.setStatus("APPROVED");
        joinRequestRepository.save(req);

        if (!room.getMemberIds().contains(requesterId)) {
            room.getMemberIds().add(requesterId);
            peerRoomRepository.save(room);
        }

        // Notify the requester
        notificationService.create(
                requesterId,
                NotificationType.GENERAL,
                "Join Request Approved ✅",
                "You have been approved to join \"" + room.getTopic() + "\". Start chatting!",
                "peer.html"
        );

        return ResponseEntity.ok(Map.of("message", "Approved"));
    }

    // ─── REJECT a join request ────────────────────────────────────────────────
    @PostMapping("/rooms/{roomId}/reject/{requesterId}")
    public ResponseEntity<?> rejectRequest(@PathVariable String roomId,
                                           @PathVariable String requesterId,
                                           @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Only the owner can reject requests"));
        }

        PeerJoinRequest req = joinRequestRepository.findByRoomIdAndRequesterId(roomId, requesterId).orElse(null);
        if (req == null) return ResponseEntity.notFound().build();

        req.setStatus("REJECTED");
        joinRequestRepository.save(req);

        // Notify the requester
        notificationService.create(
                requesterId,
                NotificationType.GENERAL,
                "Join Request Rejected",
                "Your request to join \"" + room.getTopic() + "\" was not approved.",
                "peer.html"
        );

        return ResponseEntity.ok(Map.of("message", "Rejected"));
    }

    // ─── KICK a member ────────────────────────────────────────────────────────
    @DeleteMapping("/rooms/{roomId}/kick/{memberId}")
    public ResponseEntity<?> kickMember(@PathVariable String roomId,
                                        @PathVariable String memberId,
                                        @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Only the owner can kick members"));
        }
        if (memberId.equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Owner cannot kick themselves"));
        }

        room.getMemberIds().remove(memberId);
        peerRoomRepository.save(room);

        // Notify kicked user
        notificationService.create(
                memberId,
                NotificationType.GENERAL,
                "Removed from Room",
                "You have been removed from the room \"" + room.getTopic() + "\".",
                "peer.html"
        );

        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    // ─── LEAVE a room ─────────────────────────────────────────────────────────
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId,
                                       @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (room.getOwnerId().equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Owner cannot leave. Delete the room instead."));
        }
        room.getMemberIds().remove(userId);
        peerRoomRepository.save(room);
        return ResponseEntity.ok(Map.of("message", "Left room"));
    }

    // ─── DELETE a room (owner only) ───────────────────────────────────────────
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<?> deleteRoom(@PathVariable String roomId,
                                        @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Only the owner can delete the room"));
        }
        room.setActive(false);
        peerRoomRepository.save(room);
        return ResponseEntity.ok(Map.of("message", "Room deleted"));
    }

    // ─── GET pending join requests for owner ──────────────────────────────────
    @GetMapping("/rooms/{roomId}/requests")
    public ResponseEntity<?> getPendingRequests(@PathVariable String roomId,
                                                @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getOwnerId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Only owner can see requests"));
        }
        List<PeerJoinRequest> requests = joinRequestRepository.findByRoomIdAndStatus(roomId, "PENDING");
        return ResponseEntity.ok(requests.stream().map(r -> Map.of(
                "id",            r.getId(),
                "requesterId",   r.getRequesterId(),
                "requesterName", r.getRequesterName(),
                "requestedAt",   r.getRequestedAt().toString()
        )).collect(Collectors.toList()));
    }

    // ─── GET members of a room ────────────────────────────────────────────────
    @GetMapping("/rooms/{roomId}/members")
    public ResponseEntity<?> getMembers(@PathVariable String roomId,
                                        @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getMemberIds().contains(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Not a member"));
        }
        List<Map<String, Object>> members = room.getMemberIds().stream().map(mid -> {
            User u = userRepository.findById(mid).orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",      mid);
            m.put("name",    u != null ? u.getName() : "Unknown");
            m.put("isOwner", mid.equals(room.getOwnerId()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(members);
    }

    // ─── SEND a message ───────────────────────────────────────────────────────
    @PostMapping("/rooms/{roomId}/message")
    public ResponseEntity<?> sendMessage(@PathVariable String roomId,
                                         @RequestBody MessageRequest req,
                                         @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getMemberIds().contains(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Not a member of this room"));
        }

        User user = userRepository.findById(userId).orElse(null);
        boolean anon = req.anonymous() != null ? req.anonymous() : (user != null && user.isPeerAnonymous());
        String displayName = anon ? "Anonymous" : (user != null ? user.getName() : "Peer");
        String initials    = anon ? "?" : getInitials(user != null ? user.getName() : "P");

        PeerMessage msg = PeerMessage.builder()
                .roomId(roomId)
                .senderId(userId)
                .senderName(displayName)
                .senderInitials(initials)
                .anonymous(anon)
                .content(req.content())
                .sentAt(LocalDateTime.now())
                .build();
        peerMessageRepository.save(msg);

        // Notify all OTHER members via bell
        String senderLabel = anon ? "Someone" : displayName;
        for (String memberId : room.getMemberIds()) {
            if (!memberId.equals(userId)) {
                notificationService.create(
                        memberId,
                        NotificationType.GENERAL,
                        "New message in " + room.getTopic(),
                        senderLabel + ": " + truncate(req.content(), 60),
                        "peer.html"
                );
            }
        }

        return ResponseEntity.ok(Map.of("message", "Sent", "id", msg.getId()));
    }

    // ─── GET messages (polling) ───────────────────────────────────────────────
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable String roomId,
                                         @RequestParam(required = false) String after,
                                         @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        PeerRoom room = peerRoomRepository.findById(roomId).orElse(null);
        if (room == null) return ResponseEntity.notFound().build();
        if (!room.getMemberIds().contains(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Not a member"));
        }

        List<PeerMessage> messages = after != null && !after.isBlank()
                ? peerMessageRepository.findByRoomIdAndSentAtAfterOrderBySentAtAsc(roomId, LocalDateTime.parse(after))
                : peerMessageRepository.findByRoomIdOrderBySentAtAsc(roomId);

        return ResponseEntity.ok(messages.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id",             m.getId());
            map.put("senderId",       m.getSenderId());
            map.put("senderName",     m.getSenderName());
            map.put("senderInitials", m.getSenderInitials());
            map.put("anonymous",      m.isAnonymous());
            map.put("content",        m.getContent());
            map.put("sentAt",         m.getSentAt().toString());
            map.put("isMe",           m.getSenderId().equals(userId));
            return map;
        }).collect(Collectors.toList()));
    }

    // ─── UPDATE anonymity preference ──────────────────────────────────────────
    @PutMapping("/anonymous")
    public ResponseEntity<?> setAnonymous(@RequestBody AnonRequest req,
                                          @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setPeerAnonymous(req.anonymous());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("peerAnonymous", req.anonymous()));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private Map<String, Object> buildRoomMap(PeerRoom r, String currentUserId) {
        boolean isOwner  = r.getOwnerId() != null && r.getOwnerId().equals(currentUserId);
        boolean isMember = r.getMemberIds().contains(currentUserId);
        boolean isFull   = r.getMaxMembers() > 0 && r.getMemberIds().size() >= r.getMaxMembers();

        // Check if current user has a pending request
        boolean hasPending = joinRequestRepository
                .findByRoomIdAndRequesterId(r.getId(), currentUserId)
                .map(req -> "PENDING".equals(req.getStatus())).orElse(false);

        int pendingCount = isOwner
                ? joinRequestRepository.findByRoomIdAndStatus(r.getId(), "PENDING").size()
                : 0;

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",           r.getId());
        m.put("topic",        r.getTopic());
        m.put("description",  r.getDescription());
        m.put("icon",         r.getIcon());
        m.put("category",     r.getCategory());
        m.put("visibility",   r.getVisibility());
        m.put("maxMembers",   r.getMaxMembers());
        m.put("memberCount",  r.getMemberIds().size());
        m.put("ownerName",    r.getOwnerName());
        m.put("ownerId",      r.getOwnerId());
        m.put("isOwner",      isOwner);
        m.put("isMember",     isMember);
        m.put("isFull",       isFull);
        m.put("hasPending",   hasPending);
        m.put("pendingCount", pendingCount);
        m.put("messageCount", peerMessageRepository.countByRoomId(r.getId()));
        m.put("createdAt",    r.getCreatedAt().toString());
        return m;
    }

    private String iconForCategory(String category) {
        if (category == null) return "💬";
        return switch (category) {
            case "Work Stress"       -> "💼";
            case "Sleep Issues"      -> "🌙";
            case "Anxiety & Overwhelm" -> "🌊";
            case "General Wellness"  -> "🌱";
            case "Motivation & Focus"-> "🎯";
            default                  -> "💬";
        };
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        return sb.substring(0, Math.min(2, sb.length()));
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return ""; }
    }

    public record CreateRoomRequest(String name, String description, String category,
                                    String visibility, int maxMembers) {}
    public record MessageRequest(String content, Boolean anonymous) {}
    public record AnonRequest(boolean anonymous) {}
}
