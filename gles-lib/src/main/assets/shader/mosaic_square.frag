precision highp float;
uniform sampler2D uTextureUnit;
varying highp vec2 vTextureCoord;
uniform float uTextureWidth;
uniform float uTextureHeight;
uniform float uMosaicSize;

void main() {
    vec2 uv = vTextureCoord.xy;
    float dx = uMosaicSize / uTextureWidth;
    float dy = uMosaicSize / uTextureHeight;
    vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
    gl_FragColor = vec4(texture2D(uTextureUnit, coord).xyz, 1.0);
}