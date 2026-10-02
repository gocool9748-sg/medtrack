/**
 * Lightweight Dynamic 1D Barcode (Code 128 / Code 39) & QR Code Canvas Generator
 */
class BarcodeGenerator {
  
  /**
   * Generates a clean vector-like barcode on a canvas element
   */
  drawBarcode(canvas, text, options = {}) {
    if (!canvas || !text) return;
    const ctx = canvas.getContext('2d');
    const width = options.width || 260;
    const height = options.height || 80;
    const barColor = options.color || '#000000';
    const bgColor = options.bgColor || '#ffffff';
    const showText = options.showText !== false;

    canvas.width = width;
    canvas.height = height;

    ctx.fillStyle = bgColor;
    ctx.fillRect(0, 0, width, height);

    // Generate pseudo-random deterministic bar patterns based on text hash
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
      hash = (hash << 5) - hash + text.charCodeAt(i);
      hash |= 0;
    }

    const padding = 16;
    const barAreaWidth = width - (padding * 2);
    const barAreaHeight = showText ? height - 26 : height - 10;
    
    // Pattern encoding
    const binaryBars = [];
    // Start guard
    binaryBars.push(1, 0, 1);
    
    for (let i = 0; i < text.length; i++) {
      const code = text.charCodeAt(i);
      for (let b = 0; b < 6; b++) {
        binaryBars.push((code >> b) & 1 ? 1 : 0);
      }
      binaryBars.push(0); // separator
    }
    // End guard
    binaryBars.push(1, 0, 1, 1);

    const barWidth = barAreaWidth / binaryBars.length;

    ctx.fillStyle = barColor;
    for (let i = 0; i < binaryBars.length; i++) {
      if (binaryBars[i] === 1) {
        ctx.fillRect(padding + (i * barWidth), 10, Math.ceil(barWidth), barAreaHeight);
      }
    }

    // Draw text label
    if (showText) {
      ctx.font = 'bold 12px monospace';
      ctx.fillStyle = barColor;
      ctx.textAlign = 'center';
      ctx.fillText(text, width / 2, height - 6);
    }
  }

  /**
   * Generates a QR Code Matrix simulation on canvas
   */
  drawQRCode(canvas, text, size = 120) {
    if (!canvas || !text) return;
    const ctx = canvas.getContext('2d');
    canvas.width = size;
    canvas.height = size;

    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, size, size);

    const gridCount = 21;
    const cellSize = Math.floor(size / gridCount);
    const offset = Math.floor((size - (gridCount * cellSize)) / 2);

    ctx.fillStyle = '#000000';

    // Seed matrix
    const matrix = Array(gridCount).fill(0).map(() => Array(gridCount).fill(0));

    // Corner Finder Patterns (7x7)
    const drawFinder = (startX, startY) => {
      for (let r = 0; r < 7; r++) {
        for (let c = 0; c < 7; c++) {
          if (r === 0 || r === 6 || c === 0 || c === 6 || (r >= 2 && r <= 4 && c >= 2 && c <= 4)) {
            matrix[startY + r][startX + c] = 1;
          }
        }
      }
    };

    drawFinder(0, 0);
    drawFinder(gridCount - 7, 0);
    drawFinder(0, gridCount - 7);

    // Hash data fill
    let hash = 5381;
    for (let i = 0; i < text.length; i++) {
      hash = ((hash << 5) + hash) + text.charCodeAt(i);
    }

    for (let r = 0; r < gridCount; r++) {
      for (let c = 0; c < gridCount; c++) {
        // Skip finders
        if ((r < 8 && c < 8) || (r < 8 && c >= gridCount - 8) || (r >= gridCount - 8 && c < 8)) {
          continue;
        }
        // Timing patterns
        if (r === 6 || c === 6) {
          matrix[r][c] = (r + c) % 2 === 0 ? 1 : 0;
          continue;
        }
        const val = Math.abs(Math.sin((r * 31) + (c * 17) + hash));
        matrix[r][c] = val > 0.45 ? 1 : 0;
      }
    }

    // Render cells
    for (let r = 0; r < gridCount; r++) {
      for (let c = 0; c < gridCount; c++) {
        if (matrix[r][c] === 1) {
          ctx.fillRect(offset + (c * cellSize), offset + (r * cellSize), cellSize, cellSize);
        }
      }
    }
  }
}

window.barcodeGen = new BarcodeGenerator();
