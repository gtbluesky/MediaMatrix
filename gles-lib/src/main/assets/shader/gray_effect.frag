// 黑白特效
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;

void main() {
    vec4 textureColor = texture2D(uTextureUnit, vTextureCoord);
    float gray = textureColor.r * 0.299 + textureColor.g * 0.587 + textureColor.b * 0.114;
    gl_FragColor = vec4(gray, gray, gray, 1.0);
}