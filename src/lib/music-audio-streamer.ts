export class MusicAudioStreamer {
  private context: AudioContext;
  private gainNode: GainNode;
  private audioQueue: Float32Array[][] = []; // [left, right] pairs
  private isPlaying = false;
  private scheduledTime = 0;
  private checkInterval: number | null = null;
  private readonly sampleRate = 48000;
  private readonly bufferSize = 4800; // 100ms at 48kHz

  constructor() {
    this.context = new AudioContext({ sampleRate: this.sampleRate });
    this.gainNode = this.context.createGain();
    this.gainNode.gain.value = 0.3;
    this.gainNode.connect(this.context.destination);
  }

  addPCM16Stereo(data: Uint8Array): void {
    const dataView = new DataView(data.buffer, data.byteOffset, data.byteLength);
    const numSamples = Math.floor(data.byteLength / 4); // 2 bytes * 2 channels

    let leftBuf: number[] = [];
    let rightBuf: number[] = [];

    for (let i = 0; i < numSamples; i++) {
      const left = dataView.getInt16(i * 4, true) / 32768;
      const right = dataView.getInt16(i * 4 + 2, true) / 32768;
      leftBuf.push(left);
      rightBuf.push(right);

      if (leftBuf.length >= this.bufferSize) {
        this.audioQueue.push([
          new Float32Array(leftBuf),
          new Float32Array(rightBuf),
        ]);
        leftBuf = [];
        rightBuf = [];
      }
    }

    if (leftBuf.length > 0) {
      this.audioQueue.push([
        new Float32Array(leftBuf),
        new Float32Array(rightBuf),
      ]);
    }

    if (!this.isPlaying) {
      this.isPlaying = true;
      this.scheduledTime = this.context.currentTime + 0.1;
      this.scheduleNextBuffer();
    }
  }

  private scheduleNextBuffer(): void {
    const SCHEDULE_AHEAD = 0.2;

    while (
      this.audioQueue.length > 0 &&
      this.scheduledTime < this.context.currentTime + SCHEDULE_AHEAD
    ) {
      const [left, right] = this.audioQueue.shift()!;
      const audioBuffer = this.context.createBuffer(
        2,
        left.length,
        this.sampleRate
      );
      audioBuffer.getChannelData(0).set(left);
      audioBuffer.getChannelData(1).set(right);

      const source = this.context.createBufferSource();
      source.buffer = audioBuffer;
      source.connect(this.gainNode);

      const startTime = Math.max(this.scheduledTime, this.context.currentTime);
      source.start(startTime);
      this.scheduledTime = startTime + audioBuffer.duration;
    }

    if (this.audioQueue.length === 0) {
      if (!this.checkInterval) {
        this.checkInterval = window.setInterval(() => {
          if (this.audioQueue.length > 0) {
            this.scheduleNextBuffer();
          }
        }, 100);
      }
    } else {
      const nextCheck =
        (this.scheduledTime - this.context.currentTime) * 1000;
      setTimeout(() => this.scheduleNextBuffer(), Math.max(0, nextCheck - 50));
    }
  }

  duck(): void {
    this.gainNode.gain.linearRampToValueAtTime(
      0.1,
      this.context.currentTime + 0.3
    );
  }

  unduck(): void {
    this.gainNode.gain.linearRampToValueAtTime(
      0.3,
      this.context.currentTime + 0.3
    );
  }

  setVolume(volume: number): void {
    this.gainNode.gain.setValueAtTime(volume, this.context.currentTime);
  }

  stop(): void {
    this.isPlaying = false;
    this.audioQueue = [];
    if (this.checkInterval) {
      clearInterval(this.checkInterval);
      this.checkInterval = null;
    }
    this.gainNode.gain.linearRampToValueAtTime(
      0,
      this.context.currentTime + 0.1
    );
    setTimeout(() => {
      this.gainNode.disconnect();
      this.gainNode = this.context.createGain();
      this.gainNode.gain.value = 0.3;
      this.gainNode.connect(this.context.destination);
    }, 200);
  }

  async resume(): Promise<void> {
    if (this.context.state === "suspended") {
      await this.context.resume();
    }
  }
}
