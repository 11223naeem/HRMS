const $ = id => document.getElementById(id);
const video = $('cam');
const steps = ['Look straight at the camera', 'Turn your head slightly LEFT', 'Turn your head slightly RIGHT',
               'Look straight again (optional)', 'Relax / small smile (optional)'];
let samples = [];

const say = (t, c) => { $('status').textContent = t; $('status').className = 'status ' + (c || ''); };

function refresh() {
  $('count').textContent = samples.length;
  $('capture').disabled = samples.length >= 5 || !video.srcObject;
  $('regBtn').disabled = samples.length < 3;
  if (!video.srcObject) return;
  if (samples.length < 5) say('Sample ' + (samples.length + 1) + ': ' + steps[samples.length] + ', then click Capture');
  else say('All samples captured', 'ok');
}

$('start').onclick = async () => {
  try {
    say('Loading face models...');
    await loadModels();
    await startCam(video);
    $('camBox').hidden = false;
    refresh();
  } catch (e) { say(e.message, 'err'); }
};

$('capture').onclick = async () => {
  try {
    const f = await detectOne(video);          // rejects 0 or 2+ faces
    samples.push(Array.from(f.descriptor));
    refresh();
  } catch (e) { say(e.message, 'err'); }
};

$('reset').onclick = () => { samples = []; refresh(); };

$('regForm').onsubmit = e => {
  if (samples.length < 3) { e.preventDefault(); say('Capture at least 3 samples', 'err'); return; }
  $('faceDescriptors').value = JSON.stringify(samples);
  stopCam(video);
};

$('role').onchange = () => { $('mgrBox').hidden = $('role').value !== 'MANAGER'; };
$('role').onchange();
