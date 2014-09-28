#version 150

in vec3 position;
in vec3 normal;
in vec4 color;
in vec2 texcoord;

uniform mat4 modelViewMatrix;
uniform mat4 projMatrix;

uniform vec4 lightDir;
uniform vec4 lightColor;

out vec4 Color;
out vec2 Texcoord;
out float Light;
out float flogz;

uniform float Fcoef;

void main() {
  gl_Position = projMatrix * modelViewMatrix * vec4(position, 1.0);
  gl_Position.z = log2(max(1e-6, 1.0 + gl_Position.w)) * Fcoef - 1.0;
  gl_Position.z *= gl_Position.w;

  flogz = 1.0 + gl_Position.w;

  vec4 normCamSpace = normalize(modelViewMatrix * vec4(normal, 0.0));
  float cosAngIncidence = clamp(dot(normCamSpace, lightDir), 0, 1);

  Color = lightColor*color;
  Light = cosAngIncidence;
  Texcoord = texcoord;
}