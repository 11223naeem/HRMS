// Shared helpers for face-api.js (loaded from CDN before this file).
const FACE_MODELS = 'https://cdn.jsdelivr.net/npm/@vladmandic/face-api@1.7.13/model/';
let modelsReady = false;

const sleep = ms => new Promise(res => setTimeout(res, ms));
const pdist = (a, b) => Math.hypot(a.x - b.x, a.y - b.y);
const ear = e => (pdist(e[1], e[5]) + pdist(e[2], e[4])) / (2 * pdist(e[0], e[3])); // eye aspect ratio
const detOpts = () => new faceapi.TinyFaceDetectorOptions({ inputSize: 320, scoreThreshold: 0.5 });

async function loadModels() {
  if (modelsReady) return;
  await faceapi.nets.tinyFaceDetector.loadFromUri(FACE_MODELS);
  await faceapi.nets.faceLandmark68Net.loadFromUri(FACE_MODELS);
  await faceapi.nets.faceRecognitionNet.loadFromUri(FACE_MODELS);
  modelsReady = true;
}

async function startCam(video) {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia)
    throw new Error('Camera needs HTTPS or http://localhost');
  video.srcObject = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: 640, height: 480 }, audio: false });
  await video.play();
}

function stopCam(video) {
  if (video.srcObject) video.srcObject.getTracks().forEach(t => t.stop());
  video.srcObject = null;
}

// Exactly one face must be visible; returns {detection, landmarks, descriptor}.
async function detectOne(video) {
  const all = await faceapi.detectAllFaces(video, detOpts()).withFaceLandmarks().withFaceDescriptors();
  if (all.length === 0) throw new Error('No face detected - face the camera in good light');
  if (all.length > 1) throw new Error('More than one face detected');
  return all[0];
}

function getPos() {
  return new Promise((ok, no) => {
    if (!navigator.geolocation) return no(new Error('Geolocation not supported'));
    navigator.geolocation.getCurrentPosition(p => ok(p.coords),
      e => no(new Error('Location permission needed: ' + e.message)),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 });
  });
}

// Liveness: pass if the user blinks (eyes close then open) or turns the head, within 10 s.
async function liveness(video, say) {
  say('Liveness check: blink or turn your head slowly...');
  const t0 = Date.now();
  let closed = false, yMin = 1, yMax = 0;
  while (Date.now() - t0 < 10000) {
    const r = await faceapi.detectSingleFace(video, detOpts()).withFaceLandmarks();
    if (r) {
      const lm = r.landmarks;
      const e = (ear(lm.getLeftEye()) + ear(lm.getRightEye())) / 2;
      if (e < 0.21) closed = true;
      else if (closed && e > 0.26) return true;              // blink completed
      const jaw = lm.getJawOutline(), nose = lm.getNose()[3];
      const yaw = (nose.x - jaw[0].x) / (jaw[16].x - jaw[0].x); // ~0.5 when facing the camera
      yMin = Math.min(yMin, yaw); yMax = Math.max(yMax, yaw);
      if (yMax - yMin > 0.18) return true;                   // head turned
    }
    await sleep(80);
  }
  return false;
}
