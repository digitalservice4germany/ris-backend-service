import{Y as i,i as n}from"./index-f3f720fc.js";import{c as u,e as c}from"./operations-fdeb2259.js";const v=i("loaded-norm",()=>{const a=n(void 0);async function o(e){const t=await u(e);a.value=t.data}async function r(){if(a.value){const{guid:e,articles:t,files:l,metadataSections:s,...d}=a.value;return c(a.value.guid,s??{},d)}else return{status:404,data:void 0}}return{loadedNorm:a,load:o,update:r}});export{v as u};
