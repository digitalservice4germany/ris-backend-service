import{d as a,o,c as n,u as r,a as c}from"./index-f3f720fc.js";import{d as s}from"./documentUnitService-1fe62a03.js";import"./dayjs.min-633259b2.js";const f=a({__name:"new",setup(m){const t=r();return o(async()=>{const e=await s.createNew("KO","RE");e.data&&await t.replace({name:"caselaw-documentUnit-:documentNumber-files",params:{documentNumber:e.data.documentNumber}})}),(e,u)=>(c(),n("div"))}});export{f as default};
