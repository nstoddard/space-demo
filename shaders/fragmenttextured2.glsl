#version 150

in vec4 Color;
in vec2 Texcoord;
in float flogz;

out vec4 outColor;

uniform sampler2D tex;

uniform float Fcoef_half;

void main() {
  gl_FragDepth = log2(flogz) * Fcoef_half;
  outColor = texture(tex, Texcoord) * Color;
}
