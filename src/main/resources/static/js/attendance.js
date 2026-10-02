const video = document.getElementById('cam');
const camBox = document.getElementById('camBox');
const st = document.getElementById('status');
const say = (t, c) => { st.textContent = t; st.className = 'status ' + (c || ''); };

async function run(btn) {
  const buttons = document.querySelectorAll('.fbtn');
  buttons.forEach(b => b.disabled = true);
  camBox.hidden = false;
  try {
    say('Loading face models...');
    await loadModels();
    say('Starting camera...');
    await startCam(video);
    say('Look at the camera...');
    await detectOne(video);                                   // one face must be visible
    if (!(await liveness(video, say))) throw new Error('Liveness check failed - blink or turn your head');
    say('Look straight at the camera...');
    await sleep(1200);
    const f = await detectOne(video);                         // descriptor used for matching
    say('Getting your location...');
    const pos = await getPos();
    say('Verifying...');
    const res = await fetch(btn.dataset.url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ descriptor: Array.from(f.descriptor), lat: pos.latitude, lng: pos.longitude })
    });
    const j = await res.json();
    say(j.message, j.ok ? 'ok' : 'err');
    if (j.ok) setTimeout(() => location.reload(), 3500);
  } catch (e) {
    say(e.message, 'err');
  } finally {
    stopCam(video);
    buttons.forEach(b => b.disabled = false);
  }
}
