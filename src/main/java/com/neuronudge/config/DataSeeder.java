package com.neuronudge.config;

import com.neuronudge.model.Nudge;
import com.neuronudge.model.Nudge.NudgeType;
import com.neuronudge.model.PeerRoom;
import com.neuronudge.model.User;
import com.neuronudge.repository.NudgeRepository;
import com.neuronudge.repository.PeerRoomRepository;
import com.neuronudge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final NudgeRepository nudgeRepository;
    private final PeerRoomRepository peerRoomRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (nudgeRepository.count() == 0) {
            seedNudges();
            log.info("Nudge data seeded successfully.");
        }
        if (peerRoomRepository.count() == 0) {
            seedPeerRooms();
            log.info("Peer rooms seeded successfully.");
        }
        seedAdmins();
    }

    private void seedAdmins() {
        String[][] admins = {
                {"Admin Group 1", "admin1@neuronudge.com", "admin123"},
                {"Admin Group 2", "admin2@neuronudge.com", "admin123"},
                {"Admin Group 3", "admin3@neuronudge.com", "admin123"},
                {"Admin Group 4", "admin4@neuronudge.com", "admin123"},
                {"Admin Group 5", "admin5@neuronudge.com", "admin123"}
        };
        for (String[] a : admins) {
            if (!userRepository.existsByEmail(a[1])) {
                userRepository.save(User.builder()
                        .name(a[0]).email(a[1])
                        .password(passwordEncoder.encode(a[2]))
                        .role(User.Role.ADMIN)
                        .active(true)
                        .createdAt(LocalDateTime.now())
                        .build());
                log.info("Admin seeded: {}", a[1]);
            }
        }
    }

    private void seedNudges() {
        List<Nudge> nudges = List.of(
            // BREATHE nudges (good for high burnout 60-100)
            Nudge.builder().title("Box Breathing").description("4-4-4-4 breath cycle to calm your nervous system")
                .instruction("Breathe in for 4 counts. Hold for 4. Breathe out for 4. Hold for 4. Repeat. This activates your parasympathetic nervous system and reduces stress hormones within minutes.")
                .type(NudgeType.BREATHE).durationSeconds(240).icon("🫁").color("#4f8ef7")
                .minBurnout(0).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            Nudge.builder().title("Deep Belly Breathing").description("Slow diaphragmatic breaths to release tension")
                .instruction("Place one hand on your chest, one on your belly. Breathe in slowly through your nose for 5 counts — feel your belly rise, not your chest. Breathe out for 6 counts. This is the fastest way to signal safety to your brain.")
                .type(NudgeType.BREATHE).durationSeconds(180).icon("🌬️").color("#4f8ef7")
                .minBurnout(40).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            Nudge.builder().title("4-7-8 Relaxation Breath").description("Ancient breathing technique for instant calm")
                .instruction("Inhale through your nose for 4 seconds. Hold your breath for 7 seconds. Exhale completely through your mouth for 8 seconds. Repeat 4 times. Dr. Andrew Weil calls this a natural tranquilizer for the nervous system.")
                .type(NudgeType.BREATHE).durationSeconds(300).icon("💨").color("#4f8ef7")
                .minBurnout(60).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            // MEDITATE (good for burnout 50-100)
            Nudge.builder().title("2-Minute Mindfulness").description("Quick present-moment awareness reset")
                .instruction("Sit comfortably and close your eyes. Notice 5 things you can hear, 4 you can feel, 3 you can see (mentally). Let thoughts pass like clouds without following them. When your mind wanders, gently return to the present. That is the practice — not stopping thoughts, but returning.")
                .type(NudgeType.MEDITATE).durationSeconds(120).icon("🧘").color("#c084fc")
                .minBurnout(0).maxBurnout(100).active(true).soundscapeHint("forest").build(),

            Nudge.builder().title("Body Scan").description("Release physical tension stored in your body")
                .instruction("Start at the top of your head. Slowly move your awareness downward — scalp, forehead, jaw (unclench it), shoulders (drop them), chest, hands. Notice tension without judging it. Breathe into each area. Your body holds stress you may not consciously feel.")
                .type(NudgeType.MEDITATE).durationSeconds(300).icon("🌊").color("#c084fc")
                .minBurnout(50).maxBurnout(100).active(true).soundscapeHint("rain").build(),

            Nudge.builder().title("Loving Kindness").description("Cultivate compassion to counter burnout isolation")
                .instruction("Silently repeat: 'May I be well. May I be safe. May I be peaceful.' Then extend this to someone you care about, then a neutral person, then all beings. Research shows this practice reduces stress and increases feelings of connection within 7 minutes.")
                .type(NudgeType.MEDITATE).durationSeconds(420).icon("💛").color("#c084fc")
                .minBurnout(0).maxBurnout(60).active(true).soundscapeHint("singing-bowls").build(),

            // WALK (good for low-medium burnout 0-65)
            Nudge.builder().title("5-Minute Fresh Air Walk").description("Step outside and reset your environment")
                .instruction("Leave your desk and walk outside for 5 minutes. Look at something 20 feet or further away (relieves eye strain). Notice the temperature of the air on your skin. Walking outdoors reduces cortisol more than indoor walking — even 5 minutes makes a measurable difference.")
                .type(NudgeType.WALK).durationSeconds(300).icon("🚶").color("#2dd4bf")
                .minBurnout(0).maxBurnout(65).active(true).soundscapeHint("nature").build(),

            Nudge.builder().title("Mindful Walk").description("Walking meditation — movement without screens")
                .instruction("Walk at a natural pace. Feel each footstep — heel, ball, toes. Match your breath to your steps: 3 steps inhale, 3 steps exhale. Leave your phone behind. Notice colours, sounds, textures around you. The goal is not distance — it is presence.")
                .type(NudgeType.WALK).durationSeconds(600).icon("🌿").color("#2dd4bf")
                .minBurnout(0).maxBurnout(50).active(true).soundscapeHint("nature").build(),

            // HYDRATE
            Nudge.builder().title("Hydration Reset").description("Dehydration worsens stress — drink water now")
                .instruction("Get up, fill a full glass of water, and drink it slowly. Did you know even mild dehydration (1-2%) increases cortisol levels and reduces cognitive performance? If your urine is darker than pale yellow, you are dehydrated. Make this a habit at every screen break.")
                .type(NudgeType.HYDRATE).durationSeconds(60).icon("💧").color("#38bdf8")
                .minBurnout(0).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            Nudge.builder().title("Herbal Tea Pause").description("A warm drink ritual to slow down for 5 minutes")
                .instruction("Make yourself a cup of herbal tea (chamomile, peppermint, or green tea). Hold the warm cup in both hands. Breathe in the steam. Take 3 deliberate slow sips before doing anything else. This small ritual creates a sensory anchor that signals your nervous system to down-regulate.")
                .type(NudgeType.HYDRATE).durationSeconds(300).icon("🍵").color("#38bdf8")
                .minBurnout(40).maxBurnout(100).active(true).soundscapeHint("rain").build(),

            // STRETCH
            Nudge.builder().title("Neck & Shoulder Release").description("Release tension from screen time posture")
                .instruction("Slowly tilt your right ear to your right shoulder. Hold 20 seconds. Switch sides. Then gently roll your shoulders backward 5 times. Finally, interlace your fingers behind your head and gently look up for 10 seconds. Sitting for long periods compresses the spine — this releases 80% of the tension accumulated.")
                .type(NudgeType.STRETCH).durationSeconds(120).icon("🤸").color("#f97316")
                .minBurnout(0).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            Nudge.builder().title("20-20-20 Eye Rest").description("Prevent digital eye strain with this proven method")
                .instruction("Every 20 minutes, look at something 20 feet away for 20 seconds. This is the only evidence-based method for preventing digital eye strain. Set this as a recurring reminder. Right now: find a window or far wall, focus on a point, and let your eyes relax for 20 seconds.")
                .type(NudgeType.EYE_REST).durationSeconds(20).icon("👁️").color("#f97316")
                .minBurnout(0).maxBurnout(100).active(true).soundscapeHint("silence").build(),

            // GRATITUDE
            Nudge.builder().title("3 Good Things").description("A research-backed positivity practice")
                .instruction("Write down or say out loud 3 specific things that went well today — even tiny ones. Not 'I am grateful for my family' but 'My coffee was perfect this morning.' Specificity matters. Dr. Martin Seligman's research found this practice alone reduces depression symptoms by 35% within 6 weeks.")
                .type(NudgeType.GRATITUDE).durationSeconds(120).icon("🙏").color("#eab308")
                .minBurnout(0).maxBurnout(70).active(true).soundscapeHint("forest").build(),

            // SOUNDSCAPE (special type — just listen to calming sounds)
            Nudge.builder().title("Rainstorm Listening").description("5 minutes of immersive rain sounds for recovery")
                .instruction("Put on headphones if you can. Close your eyes or soften your gaze. Simply listen to the rain. No agenda — not even relaxing on purpose. Let the sound wash over you. Brown noise and rain sounds are clinically shown to reduce anxiety, improve focus, and lower heart rate within 3 minutes.")
                .type(NudgeType.SOUNDSCAPE).durationSeconds(300).icon("🌧️").color("#64748b")
                .minBurnout(50).maxBurnout(100).active(true).soundscapeHint("rain").build(),

            // JOURNALING
            Nudge.builder().title("Brain Dump").description("Empty your mental load onto paper in 3 minutes")
                .instruction("Set a 3-minute timer. Write every thought in your head without editing, punctuating, or judging. Worries, to-dos, random thoughts — all of it. The act of externalising your internal mental chatter reduces cognitive load and anxiety. When the timer ends, you can tear it up or keep it — the benefit is in the writing.")
                .type(NudgeType.JOURNALING).durationSeconds(180).icon("📓").color("#a855f7")
                .minBurnout(60).maxBurnout(100).active(true).soundscapeHint("silence").build()
        );
        nudgeRepository.saveAll(nudges);
    }
    private void seedPeerRooms() {
        List<PeerRoom> rooms = List.of(
                PeerRoom.builder()
                        .topic("Work Stress")
                        .description("For those overwhelmed by deadlines, workload, and burnout from work")
                        .icon("💼").type("GROUP")
                        .memberIds(new java.util.ArrayList<>())
                        .minBurnout(60).maxBurnout(100)
                        .createdAt(java.time.LocalDateTime.now())
                        .active(true).build(),

                PeerRoom.builder()
                        .topic("Sleep Issues")
                        .description("Struggling with sleep? Connect with others facing the same challenge")
                        .icon("🌙").type("GROUP")
                        .memberIds(new java.util.ArrayList<>())
                        .minBurnout(0).maxBurnout(100)
                        .createdAt(java.time.LocalDateTime.now())
                        .active(true).build(),

                PeerRoom.builder()
                        .topic("Anxiety & Overwhelm")
                        .description("A safe space to share feelings of anxiety and being overwhelmed")
                        .icon("🌊").type("GROUP")
                        .memberIds(new java.util.ArrayList<>())
                        .minBurnout(70).maxBurnout(100)
                        .createdAt(java.time.LocalDateTime.now())
                        .active(true).build(),

                PeerRoom.builder()
                        .topic("General Wellness")
                        .description("Casual conversations about staying healthy, balanced, and happy")
                        .icon("🌱").type("GROUP")
                        .memberIds(new java.util.ArrayList<>())
                        .minBurnout(0).maxBurnout(50)
                        .createdAt(java.time.LocalDateTime.now())
                        .active(true).build(),

                PeerRoom.builder()
                        .topic("Motivation & Focus")
                        .description("Struggling to stay motivated or focused? You are not alone")
                        .icon("🎯").type("GROUP")
                        .memberIds(new java.util.ArrayList<>())
                        .minBurnout(30).maxBurnout(75)
                        .createdAt(java.time.LocalDateTime.now())
                        .active(true).build()
        );
        peerRoomRepository.saveAll(rooms);
    }
}
