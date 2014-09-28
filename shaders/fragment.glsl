#version 150

in vec4 Color;
in float flogz;

out vec4 outColor;

uniform float Fcoef_half;

void main() {
  gl_FragDepth = log2(flogz) * Fcoef_half;
  outColor = Color;
}
