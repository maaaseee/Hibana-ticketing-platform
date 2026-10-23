const fs = require('fs');
const path = require('path');

let pathsSvg = '';
for (let i = 0; i < 30; i++) {
  let t = i / 29;
  let yBase = 65 + i * 9.5;
  let cp1x = 200 + i * 22;
  let cp1y = yBase - 90 + Math.sin(i * 0.42) * 38;
  let cp2x = 800 - i * 14;
  let cp2y = yBase + 105 - Math.cos(i * 0.42) * 48;
  let midX = 1200 + i * 15;
  let midY = yBase - 30 + Math.sin(i * 0.58) * 32;
  let cp3x = 1600 - i * 12;
  let cp3y = yBase + 85 - Math.cos(i * 0.48) * 44;
  let cp4x = 2000 + i * 10;
  let cp4y = yBase - 60 + Math.sin(i * 0.38) * 32;
  let startX = -500;
  let endX = 2500;
  let d = `M ${startX} ${yBase.toFixed(1)} C ${cp1x.toFixed(1)} ${cp1y.toFixed(1)}, ${cp2x.toFixed(1)} ${cp2y.toFixed(1)}, ${midX.toFixed(1)} ${midY.toFixed(1)} C ${cp3x.toFixed(1)} ${cp3y.toFixed(1)}, ${cp4x.toFixed(1)} ${cp4y.toFixed(1)}, ${endX} ${yBase.toFixed(1)}`;
  let opacity = (0.22 + (1 - Math.abs(t - 0.5) * 1.4) * 0.72).toFixed(2);
  let strokeWidth = (1.1 + (1 - Math.abs(t - 0.5)) * 1.3).toFixed(2);
  let grad = i % 2 === 0 ? 'url(#wave-flow-primary)' : 'url(#wave-flow-secondary)';
  let groupClass = i < 10 ? 'anim-wave-layer-1' : (i < 20 ? 'anim-wave-layer-2' : 'anim-wave-layer-3');
  pathsSvg += `    <path d="${d}" stroke="${grad}" stroke-width="${strokeWidth}" opacity="${opacity}" class="${groupClass}" />\n`;
}

const svgContent = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 420" width="100%" height="100%" preserveAspectRatio="none">
  <defs>
    <!-- Gradiente 1: De Cian Eléctrico a Celeste, Turquesa y Violeta Suave -->
    <linearGradient id="wave-flow-primary" x1="0%" y1="20%" x2="100%" y2="80%">
      <stop offset="0%" stop-color="#00e5ff" stop-opacity="0.9" />
      <stop offset="28%" stop-color="#38bdf8" stop-opacity="0.85" />
      <stop offset="62%" stop-color="#4fdbc8" stop-opacity="0.75" />
      <stop offset="88%" stop-color="#818cf8" stop-opacity="0.55" />
      <stop offset="100%" stop-color="#00688f" stop-opacity="0.15" />
    </linearGradient>

    <!-- Gradiente 2: De Celeste Intenso a Esmeralda y Océano Profundo -->
    <linearGradient id="wave-flow-secondary" x1="100%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="#56cbf9" stop-opacity="0.85" />
      <stop offset="35%" stop-color="#0ec2d7" stop-opacity="0.78" />
      <stop offset="70%" stop-color="#14b8a6" stop-opacity="0.6" />
      <stop offset="100%" stop-color="#0b131e" stop-opacity="0.1" />
    </linearGradient>

    <!-- Resplandor Difuso Superior para Profundidad de Escenario -->
    <radialGradient id="wave-ambient-glow" cx="50%" cy="0%" r="70%">
      <stop offset="0%" stop-color="#0ec2d7" stop-opacity="0.18" />
      <stop offset="45%" stop-color="#00688f" stop-opacity="0.06" />
      <stop offset="100%" stop-color="#0b131e" stop-opacity="0" />
    </radialGradient>

    <style>
      @keyframes waveSwayBack {
        0%, 100% {
          transform: translate(0px, 0px) scaleY(1);
        }
        50% {
          transform: translate(-45px, -12px) scaleY(1.05);
        }
      }
      @keyframes waveSwayMid {
        0%, 100% {
          transform: translate(0px, 0px) scaleY(1);
        }
        50% {
          transform: translate(35px, -8px) scaleY(0.96);
        }
      }
      @keyframes waveSwayFront {
        0%, 100% {
          transform: translate(0px, 0px) scaleY(1);
        }
        50% {
          transform: translate(-30px, 10px) scaleY(1.03);
        }
      }
      @keyframes waveGlowPulse {
        0%, 100% { opacity: 0.82; }
        50% { opacity: 1; }
      }

      .anim-wave-layer-1 {
        transform-origin: 50% 20%;
        animation: waveSwayBack 20s ease-in-out infinite, waveGlowPulse 8s ease-in-out infinite;
      }
      .anim-wave-layer-2 {
        transform-origin: 50% 50%;
        animation: waveSwayMid 15s ease-in-out infinite, waveGlowPulse 6s ease-in-out infinite reverse;
      }
      .anim-wave-layer-3 {
        transform-origin: 50% 80%;
        animation: waveSwayFront 12s ease-in-out infinite, waveGlowPulse 7s ease-in-out infinite;
      }
    </style>
  </defs>

  <!-- Brillo Atmosférico Difuso en el Fondo -->
  <rect x="-500" y="0" width="3000" height="420" fill="url(#wave-ambient-glow)" />

  <!-- 30 Líneas Ondulares Acústicas con Overflow Extendido (-500 a 2500) -->
  <g fill="none" stroke-linecap="round">
${pathsSvg}  </g>
</svg>`;

const targetPath = path.resolve(__dirname, '../ticketing-frontend/public/Wave Line.svg');
fs.writeFileSync(targetPath, svgContent, 'utf-8');
console.log('Successfully wrote updated Wave Line.svg with extended overflow (-500 to 2500)!');
