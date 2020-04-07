// 灰度
precision highp float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureUnit;

void main() {
    vec4 textureColor = texture2D(uTextureUnit, vTextureCoord);
    float gray = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));
    gl_FragColor = vec4(gray, gray, gray, 1.0);
}