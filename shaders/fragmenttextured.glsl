#version 150

in vec4 Color;
in vec2 Texcoord;
in float Light;
in float flogz;

out vec4 outColor;

uniform sampler2D dayTex;
uniform sampler2D nightTex;

uniform float Fcoef_half;

void main() {
  gl_FragDepth = log2(flogz) * Fcoef_half;
  vec4 dayColor = texture(dayTex, Texcoord);
  vec4 nightColor = texture(nightTex, Texcoord);
  outColor = mix(nightColor, dayColor, Light) * vec4(Color.rgb, 1);
}
