package com.samarbhumi.audio;

import javax.sound.midi.*;


/**
 * Audio engine using Java's built-in MIDI synthesizer.
 * No external audio files — all sounds are procedurally generated.
 * BGM themes: Main Menu, Battle, Victory, GameOver.
 * SFX: gunshot (different per weapon), explosion, jump, pickup, death.
 *
 * Demonstrates: thread safety, resource management, polymorphism in sound design.
 */
public class AudioEngine {

    private Synthesizer  synth;
    private Sequencer    sequencer;
    private boolean      muted   = false;
    private float        bgmVol  = 0.7f;
    private float        sfxVol  = 0.9f;
    private int          currentTheme = -1;
    private MidiChannel[] channels;

    // Theme IDs
    public static final int THEME_MENU    = 0;
    public static final int THEME_BATTLE  = 1;
    public static final int THEME_VICTORY = 2;
    public static final int THEME_GAMEOVER= 3;

    public AudioEngine() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            channels = synth.getChannels();
            sequencer = MidiSystem.getSequencer(false);
            sequencer.open();
            // Connect sequencer → synth
            Transmitter trans = sequencer.getTransmitter();
            trans.setReceiver(synth.getReceiver());
        } catch (Exception e) {
            System.err.println("[Audio] MIDI unavailable: " + e.getMessage());
            synth = null; sequencer = null; channels = null;
        }
    }

    // ── BGM ──────────────────────────────────────────────────────────────

    public void playTheme(int theme) {
        if (sequencer == null || theme == currentTheme) return;
        currentTheme = theme;
        try {
            if (sequencer.isRunning()) sequencer.stop();
            Sequence seq = buildTheme(theme);
            sequencer.setSequence(seq);
            sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(getThemeBPM(theme));
            applyBgmVolume();
            sequencer.start();
        } catch (Exception e) { System.err.println("[Audio] BGM error: " + e.getMessage()); }
    }

    public void stopBGM() {
        if (sequencer != null && sequencer.isRunning()) sequencer.stop();
        currentTheme = -1;
    }

    private int getThemeBPM(int theme) {
        return switch (theme) {
            case THEME_MENU    -> 78;
            case THEME_BATTLE  -> 138;
            case THEME_VICTORY -> 100;
            case THEME_GAMEOVER-> 55;
            default -> 90;
        };
    }

    // ── SFX (direct MIDI channel hits — instant, no sequencer) ─────────

    public void sfxGunshot(com.samarbhumi.core.Enums.WeaponType wt) {
        if (channels == null || muted) return;
        try {
            int ch = 1; // SFX channel
            switch (wt) {
                case ASSAULT_RIFLE, SMG -> shortBlip(ch, 38, 100, 35);  // snare hit
                case SHOTGUN            -> shortBlip(ch, 36, 127, 80);  // bass drum heavy
                case SNIPER             -> shortBlip(ch, 49, 110, 120); // crash cymbal
                case ROCKET_LAUNCHER   -> {
                    shortBlip(ch, 36, 127, 90); shortBlip(9, 57, 100, 20);
                }
                case PISTOL, DUAL_PISTOLS -> shortBlip(ch, 38, 80, 25);
                default -> shortBlip(ch, 38, 70, 20);
            }
        } catch (Exception ignored) {}
    }

    public void sfxExplosion() {
        if (channels == null || muted) return;
        try {
            channels[9].noteOn(36, 127); // GM perc bass
            channels[9].noteOn(57, 100);
            new Thread(() -> {
                try { Thread.sleep(120); channels[9].noteOff(36); channels[9].noteOff(57); }
                catch (Exception ignored) {}
            }).start();
        } catch (Exception ignored) {}
    }

    public void sfxJump() {
        if (channels == null || muted) return;
        try {
            channels[2].programChange(79); // ocarina
            shortBlip(2, 72, 70, 60);
        } catch (Exception ignored) {}
    }

    public void sfxPickup() {
        if (channels == null || muted) return;
        try {
            channels[3].programChange(11); // vibraphone
            shortBlip(3, 72, 80, 40);
            new Thread(() -> {
                try { Thread.sleep(60); shortBlip(3, 76, 75, 40); } catch(Exception ignored){}
            }).start();
        } catch (Exception ignored) {}
    }

    public void sfxDeath() {
        if (channels == null || muted) return;
        try {
            channels[4].programChange(75); // recorder flute
            shortBlip(4, 60, 90, 80);
            new Thread(() -> { try {
                Thread.sleep(100); shortBlip(4, 57, 80, 100);
                Thread.sleep(100); shortBlip(4, 53, 70, 200);
            } catch(Exception ignored){} }).start();
        } catch (Exception ignored) {}
    }

    public void sfxMelee() {
        if (channels == null || muted) return;
        try { shortBlip(9, 42, 110, 50); } catch (Exception ignored) {}
    }

    public void sfxReload() {
        if (channels == null || muted) return;
        try {
            channels[5].programChange(117); // melodic tom
            shortBlip(5, 60, 70, 30);
            new Thread(() -> { try { Thread.sleep(50); shortBlip(5, 64, 60, 30); } catch(Exception ignored){} }).start();
        } catch (Exception ignored) {}
    }

    public void sfxKill() {
        if (channels == null || muted) return;
        try {
            channels[6].programChange(11);
            for (int i=0; i<3; i++) {
                final int fi=i;
                new Thread(() -> { try { Thread.sleep(fi*80L); shortBlip(6, 72+fi*4, 90, 60); } catch(Exception ignored){} }).start();
            }
        } catch (Exception ignored) {}
    }

    public void sfxLevelUp() {
        if (channels == null || muted) return;
        try {
            channels[7].programChange(11);
            int[] notes = {60,64,67,72};
            for (int i=0;i<notes.length;i++) {
                final int fi=i, fn=notes[i];
                new Thread(() -> { try { Thread.sleep(fi*100L); shortBlip(7, fn, 100, 150); } catch(Exception ignored){} }).start();
            }
        } catch (Exception ignored) {}
    }

    public void sfxMenuClick() {
        if (channels == null || muted) return;
        try { channels[8].programChange(11); shortBlip(8, 76, 65, 30); } catch(Exception ignored){}
    }

    private void shortBlip(int ch, int note, int vel, int durationMs) {
        if (channels == null) return;
        int v = (int)(vel * sfxVol);
        v = Math.max(0, Math.min(127, v));
        channels[ch].noteOn(note, v);
        final int fc=ch, fn=note;
        new Thread(() -> { try { Thread.sleep(durationMs); channels[fc].noteOff(fn); } catch(Exception ignored){} }).start();
    }

    // ── Theme builders ────────────────────────────────────────────────────

    private Sequence buildTheme(int theme) throws InvalidMidiDataException {
        return switch (theme) {
            case THEME_MENU    -> buildMenuTheme();
            case THEME_BATTLE  -> buildBattleTheme();
            case THEME_VICTORY -> buildVictoryTheme();
            case THEME_GAMEOVER-> buildGameoverTheme();
            default -> buildMenuTheme();
        };
    }

    /** Main menu: mysterious, militaristic, slower tempo */
    private Sequence buildMenuTheme() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 480);
        Track mel  = seq.createTrack();
        Track bass = seq.createTrack();
        Track pad  = seq.createTrack();

        setInstr(mel,  0, 90);  // Pad 3 polysynth
        setInstr(bass, 1, 34);  // Electric bass
        setInstr(pad,  2, 48);  // String ensemble

        // Minor key drone — Am
        int[] bassNotes = {45, 45, 48, 47, 45, 43, 45, 48};
        int[] melNotes  = {69, 72, 76, 74, 72, 69, 67, 69, 72, 76};
        int[] padChord  = {45, 52, 57, 60};

        int t = 0;
        for (int rep=0; rep<2; rep++) {
            for (int n : padChord) addNote(pad, 2, n, 32, t, 3840);
            for (int i=0; i<bassNotes.length; i++) addNote(bass, 1, bassNotes[i], 65, t+i*480, 440);
            t += 3840;
        }
        t = 0;
        for (int i=0; i<melNotes.length; i++) addNote(mel, 0, melNotes[i], 55, t+i*360+480, 320);

        return seq;
    }

    /** Battle: driving, fast, high energy military action */
    private Sequence buildBattleTheme() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 480);
        Track mel  = seq.createTrack();
        Track bass = seq.createTrack();
        Track perc = seq.createTrack();

        setInstr(mel,  0, 56);  // Trumpet
        setInstr(bass, 1, 34);  // Bass

        // Driving minor riff — Em
        int[] riff = {40, 40, 43, 40, 38, 40, 43, 45};
        int[] mel2 = {64, 62, 60, 59, 60, 62, 64, 64};
        int beat   = 180; // 16th note at 138bpm

        int t=0;
        for (int rep=0; rep<8; rep++) {
            for (int i=0; i<riff.length; i++) addNote(bass, 1, riff[i], 100, t+i*beat, beat-15);
            // Kick+snare pattern
            addNote(perc, 9, 36, 120, t,            60);
            addNote(perc, 9, 38, 100, t+beat*2,     60);
            addNote(perc, 9, 36, 110, t+beat*4,     60);
            addNote(perc, 9, 36, 90,  t+beat*6,     60);
            addNote(perc, 9, 38, 100, t+beat*6,     60);
            // Hihat 8ths
            for (int i=0; i<8; i++) addNote(perc, 9, 42, 55, t+i*beat*2, 40);
            t += beat*8;
        }
        // Melody
        t = 0;
        for (int i=0; i<mel2.length; i++) addNote(mel, 0, mel2[i], 90, t+i*beat*2, beat*2-20);

        return seq;
    }

    private Sequence buildVictoryTheme() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 480);
        Track t1 = seq.createTrack();
        setInstr(t1, 0, 56);
        int[] notes = {60,64,67,72,76,72,67,64,72};
        int[] durs  = {480,480,480,960,960,480,480,480,1440};
        int t=0;
        for (int i=0; i<notes.length; i++) { addNote(t1, 0, notes[i], 100, t, durs[i]-30); t+=durs[i]; }
        return seq;
    }

    private Sequence buildGameoverTheme() throws InvalidMidiDataException {
        Sequence seq = new Sequence(Sequence.PPQ, 480);
        Track t1 = seq.createTrack();
        setInstr(t1, 0, 70);
        int[] notes = {60,59,57,55,53,52,50,48};
        int t=0;
        for (int n : notes) { addNote(t1, 0, n, 75, t, 700); t+=720; }
        return seq;
    }

    // ── MIDI helpers ─────────────────────────────────────────────────────

    private void setInstr(Track t, int ch, int patch) throws InvalidMidiDataException {
        ShortMessage m = new ShortMessage();
        m.setMessage(ShortMessage.PROGRAM_CHANGE, ch, patch, 0);
        t.add(new MidiEvent(m, 0));
    }

    private void addNote(Track t, int ch, int pitch, int vel, int tick, int dur) throws InvalidMidiDataException {
        ShortMessage on  = new ShortMessage(); on .setMessage(ShortMessage.NOTE_ON,  ch, pitch, vel);
        ShortMessage off = new ShortMessage(); off.setMessage(ShortMessage.NOTE_OFF, ch, pitch, 0);
        t.add(new MidiEvent(on,  tick));
        t.add(new MidiEvent(off, tick+dur));
    }

    private void applyBgmVolume() {
        if (channels == null) return;
        int v = (int)(bgmVol * 127);
        v = Math.max(0, Math.min(127, v));
        for (MidiChannel c : channels) if (c!=null) c.controlChange(7, v);
    }

    // ── Controls ─────────────────────────────────────────────────────────

    public void setBgmVolume(float v)  { bgmVol=Math.max(0,Math.min(1,v)); applyBgmVolume(); }
    public void setSfxVolume(float v)  { sfxVol=Math.max(0,Math.min(1,v)); }
    public float getBgmVolume()        { return bgmVol; }
    public float getSfxVolume()        { return sfxVol; }
    public boolean isMuted()           { return muted; }
    public void setMuted(boolean m)    { muted=m; if(m && sequencer!=null && sequencer.isRunning()) sequencer.stop(); else if(!m) playTheme(currentTheme); }
    public void toggleMute()           { setMuted(!muted); }
    public int  getCurrentTheme()      { return currentTheme; }

    public void close() {
        try { if(sequencer!=null) sequencer.close(); if(synth!=null) synth.close(); }
        catch (Exception ignored) {}
    }
}
