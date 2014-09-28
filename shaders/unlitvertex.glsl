#version 150

in vec3 position;
in vec4 color;

uniform mat4 modelViewMatrix;
uniform mat4 projMatrix;

out vec4 Color;
out float flogz;

uniform float Fcoef;

void main() {
  gl_Position = projMatrix * modelViewMatrix * vec4(position, 1.0);
  gl_Position.z = log2(max(1e-6, 1.0 + gl_Position.w)) * Fcoef - 1.0;
  gl_Position.z *= gl_Position.w;

  flogz = 1.0 + gl_Position.w;

  Color = color;
}
